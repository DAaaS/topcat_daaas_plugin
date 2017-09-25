package org.icatproject.topcatdaaasplugin;

import org.icatproject.topcatdaaasplugin.cloudclient.CloudClient;
import org.icatproject.topcatdaaasplugin.cloudclient.entities.Server;
import org.icatproject.topcatdaaasplugin.database.Database;
import org.icatproject.topcatdaaasplugin.database.entities.Machine;
import org.icatproject.topcatdaaasplugin.database.entities.MachineType;
import org.icatproject.topcatdaaasplugin.exceptions.DaaasException;
import org.icatproject.topcatdaaasplugin.exceptions.UnexpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Arrays;

import java.util.concurrent.TimeUnit;

@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton
@Startup
public class MachinePool {

    private static final Logger logger = LoggerFactory.getLogger(MachinePool.class);

    @EJB
    CloudClient cloudClient;

    @EJB
    Database database;

    @PostConstruct
    public void init() {

    }

    /**
     * Periodically check the
     */
    @Schedule(hour = "*", minute = "*/1")
    public void managePool() {
        logger.debug("Checking for machine pool updates");

        try {
            EntityList<Entity> machineTypes = database.query("select machineType from MachineType machineType");

            for (Entity machineTypeEntity : machineTypes) {
                MachineType machineType = (MachineType) machineTypeEntity;
                EntityList<Entity> nonAquiredMachines = database.query("select machine from Machine machine, machine.machineType as machineType where (machine.state = 'preparing' or machine.state = 'vacant') and machineType.id = " + machineType.getId());

                int diff = machineType.getPoolSize() - nonAquiredMachines.size();

                if (diff > 0) {
                    logger.info("Adding {} machines to pool for machine type '{}'", diff, machineType.getName());
                    for (int i = 0; i < diff; i++) {
                        createMachine(machineType);
                    }
                } else if (diff < 0) {
                    logger.info("Removing {} machines from pool for machine type '{}'", diff, machineType.getName());
                    for (int i = 0; i < (diff * -1); i++) {
                        Machine machine = aquireMachine(machineType.getId());
                        deleteMachine(machine, "DELETED");
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }


    /**
     * Check to see if a machine is ready to be marked as VACANT. This will be subject to
     * three checks:
     * <p>
     * - The machine has been assigned only _one_ hostname (weird bug with the cloud).
     * <p>
     * - The AQ_STATUS metadata - which indicates if the state if of the Aquilon request -
     * is set to SUCCESS.
     * <p>
     * - The time taken for the machine to configure itself, indicated by the SSH API
     * is_ready call, has not taken longer than the maxPrepareSeconds config option.
     */
    @Schedule(hour = "*", minute = "*/1")
    public void checkPreparing() throws DaaasException, IOException, InterruptedException {
        logger.debug("Checking for machines that have finished preparing");

        try {
            EntityList<Entity> preparingMachines = database.query("select machine from Machine machine, machine.machineType as machineType where machine.state = 'preparing'");
            for (Entity machineEntity : preparingMachines) {
                Machine machine = (Machine) machineEntity;

                try {
                    long created = (new Date().getTime() - machine.getCreatedAt().getTime()) / 1000;
                    if (created < 60) {
                        logger.debug("Machine {} is to young, skipping checks.", machine.getId());
                        continue;
                    }

                    Server server = cloudClient.getServer(machine.getId());

                    // Sometimes cloud VMs will be assigned two hostnames for some reason.
                    // If this occurs, just mark the machine as failed.
                    String hostnames[] = server.getHost().split("[ ]*,[ ]*");
                    if (machine.getHost() == null && server.getHost() != null && hostnames.length == 1) {
                        logger.info("Setting hostname of machine {} to {}", machine.getId(), server.getHost());
                        machine.setHost(server.getHost());
                        database.persist(machine);
                    } else if (hostnames.length > 1) {
                        logger.error("Machine {} has more than one hostname ({}). Setting state to failed.", machine.getId(), hostnames);
                        throw new DaaasException("Machine failed");
                    }

                    // Check the Aquilon Metadata attached to the VM to see if AQ_STATUS
                    // is set as SUCCESS or FAILED (or empty)
                    if (!server.getStatus().equals("SUCCESS")) {
                        logger.error("Machine {} failed...", machine.getId());
                        throw new DaaasException("Machine failed");
                    }

                    // Check to see if the machine has taken too long to configure itself
                    Properties properties = new Properties();
                    int maxPrepareSeconds = Integer.valueOf(properties.getProperty("maxPrepareSeconds", "600"));
                    long createdSecondsAgo = (new Date().getTime() - machine.getCreatedAt().getTime()) / 1000;
                    if (createdSecondsAgo > maxPrepareSeconds) {
                        logger.error("Machine {} has taken longer than {} to configure. Setting state as failed.", machine.getId(), maxPrepareSeconds);
                        throw new DaaasException("Machine failed");
                    }

                    // Check to see if machine is actually ready. If so, mark it as VACANT so it is
                    // available to users.
                    SshClient sshClient = new SshClient(machine.getHost());
                    if (sshClient.exec("is_ready").equals("1\n")) {
                        logger.info("Machine {} has finished configuring. Setting state to vacant", machine.getId());
                        machine.setState("vacant");
                        database.persist(machine);
                    } else {
                        logger.debug("Machine {} is not ready yet", machine.getId());
                    }
                } catch (DaaasException e) {
                    logger.error("Something went wrong with the preparing machine {}. Deleting.", machine.getId());
                    deleteMachine(machine, "FAILED");
                } catch (Exception e) {
                    logger.error("Something when wrong checking a preparing machine. Skipping.");
                    logger.error(e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to check if all preparing machines are finished: {}", e.getMessage());
            throw e;
        }
    }

    @Schedule(hour = "*", minute = "*/5")
    public void getScreenShots() {
        try {
            EntityList<Entity> aquiredMachines = database.query("select machine from Machine machine where machine.state = 'aquired'");
            for (Entity machineEntity : aquiredMachines) {
                Machine machine = (Machine) machineEntity;
                machine.setScreenshot(Base64.getMimeDecoder().decode(new SshClient(machine.getHost()).exec("get_screenshot")));
                database.persist(machine);
            }
        } catch (Exception e) {
            logger.error("getScreenShots: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Schedule(hour = "*", minute = "*/1")
    public void cleanUpFailedMachines() {
        try {
            EntityList<Entity> failedMachines = database.query("select machine from Machine machine where machine.state = 'failed'");
            for (Entity machineEntity : failedMachines) {
                Machine machine = (Machine) machineEntity;
                cloudClient.deleteServer(machine.getId());
                machine.setState("failed:cleaned_up");
                database.persist(machine);
                logger.info("cleanUpFailedMachines: cleaned up failed machine, id = " + machine.getId());
            }
        } catch (Exception e) {
            logger.error("cleanUpFailedMachines: " + e.getMessage());
        }
    }

    public synchronized Machine aquireMachine(Long machineTypeId) throws DaaasException {
        try {
            String query = "select machine from Machine machine, machine.machineType as machineType where machine.state = 'vacant' and machineType.id = " + machineTypeId;
            EntityList<Entity> vacantMachines = database.query(query);
            if (vacantMachines.size() < 1) {
                return null;
            }
            Machine out = (Machine) vacantMachines.get(0);
            out.setState("aquired");
            database.persist(out);
            logger.info("Machine {} has been acquired", out.getId());
            return out;
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage());
        }
    }

    private void deleteMachine(Machine machine, String state) {
        try {
            logger.info("Deleting machine {}", machine.getId());
            cloudClient.deleteServer(machine.getId());
            machine.setState(state);
            database.persist(machine);
        } catch (Exception e) {
            logger.error("Failed to delete machine: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void createMachine(MachineType machineType) {
        try {
            logger.info("Attempting to create new machine for machineType {}", machineType.getName());
            logger.info(machineType.toJsonObjectBuilder().build().toString());

            Machine machine = new Machine();
            Map<String, String> metadata = new HashMap<String, String>();
            metadata.put("AQ_ARCHETYPE", machineType.getAquilonArchetype());
            metadata.put("AQ_DOMAIN", machineType.getAquilonDomain());
            metadata.put("AQ_PERSONALITY", machineType.getAquilonPersonality());
            metadata.put("AQ_SANDBOX", machineType.getAquilonSandbox());
            metadata.put("AQ_OSVERSION", machineType.getAquilonOSVersion());

            Server server = cloudClient.createServer(machineType.getName(), machineType.getImageId(), machineType.getFlavorId(), machineType.getAvailabilityZone(), metadata);

            machine.setId(server.getId());
            machine.setName(machineType.getName());
            machine.setState("preparing");
            machine.setMachineType(machineType);
            database.persist(machine);

            logger.info("Successfully created new machine: id={}", machine.getId());
        } catch (Exception e) {
            logger.error("Failed to create new machine: {}", e.getMessage());
            e.printStackTrace();
        }
    }

}