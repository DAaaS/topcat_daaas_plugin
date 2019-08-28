package org.icatproject.topcatdaaasplugin;

import org.icatproject.topcatdaaasplugin.database.Database;
import org.icatproject.topcatdaaasplugin.database.entities.Machine;
import org.icatproject.topcatdaaasplugin.exceptions.UnexpectedException;
import org.icatproject.topcatdaaasplugin.vmm.VmmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.*;
import java.util.HashMap;
import java.util.Map;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton
@Startup
public class LastActivity {

    private static final Logger logger = LoggerFactory.getLogger(LastActivity.class);
    private VmmClient vmmClient = new VmmClient();

    @EJB
    Database database;

    @Schedule(hour = "*", minute = "*/15")
    public void lastActivity() {
        logger.info("Running last activity check");
        logger.info("===========================");
        Properties properties = new Properties();
        long deleteTime = Long.parseLong(properties.getProperty("time_to_delete"));

        try {
            EntityList<Entity> machines = database.query("select machine from Machine machine");
    
            for (Entity machineEntity : machines) {
                try {
                    Machine machine = (Machine) machineEntity;

                    SshClient sshClient = new SshClient(machine.getHost());
        
                    logger.info("Running last activity for " + machine.getHost());
                    String lastactivity = sshClient.exec("get_last_activity").replace("\n", "");
                    logger.info("Host returned: " + lastactivity);
                    Date lastactivity_date = new Date ();
                    lastactivity_date.setTime(Long.parseLong(lastactivity)*1000);
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    logger.info("Last activity = " + dateFormat.format(lastactivity_date));

                    long currentTime = System.currentTimeMillis() / 1000L;

                    logger.info("Current time = " + String.valueOf(currentTime));

                    long difference = currentTime - Long.parseLong(lastactivity);

                    logger.info("Difference = " + String.valueOf(difference));
                    logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

                    if (difference > deleteTime) {
                        logger.info("Inactivity on machine {} is greater than deleteTime. Deleting", machine.getId());
                        database.remove(machine);
                        try {
                            vmmClient.delete_machine(machine.getId());
                        } catch (UnexpectedException e) {
                            logger.warn("Failed to delete VM - instance error? : {}", e.getMessage());
                        } catch (Exception e) {
                            throw new UnexpectedException(e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.error("Something went wrong checking last activity: {}", e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        catch(Exception e){
            logger.debug("Last activity check failed");
        }
    }
}
