package org.icatproject.topcatdaaasplugin;

import org.icatproject.topcatdaaasplugin.database.Database;
import org.icatproject.topcatdaaasplugin.database.entities.Machine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton
@Startup
public class MachinePool {

    private static final Logger logger = LoggerFactory.getLogger(MachinePool.class);

    public enum STATE {
        VACANT, PREPARING, ACQUIRED, FAILED, DELETED
    }

    @EJB
    Database database;

    @PostConstruct
    public void init() {

    }

    @Schedule(hour = "*", minute = "*/5", persistent=false)
    public void getScreenShots() {
        try {
            logger.debug("Running screenshot");
            Map<String, Object> params = new HashMap<>();
            params.put("state", STATE.ACQUIRED.name());
            EntityList<Entity> acquiredMachines = database.query("select machine from Machine machine where machine.state = :state", params);
            for (Entity machineEntity : acquiredMachines) {
                Machine machine = (Machine) machineEntity;
                machine.setScreenshot(Base64.getMimeDecoder().decode(new SshClient(machine.getHost()).exec("get_screenshot")));
                database.persist(machine);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }
}