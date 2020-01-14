package org.icatproject.topcatdaaasplugin.rest;

import org.icatproject.topcatdaaasplugin.EntityList;
import org.icatproject.topcatdaaasplugin.IcatClient;
import org.icatproject.topcatdaaasplugin.SshClient;
import org.icatproject.topcatdaaasplugin.TopcatClient;
import org.icatproject.topcatdaaasplugin.database.Database;
import org.icatproject.topcatdaaasplugin.database.entities.Machine;
import org.icatproject.topcatdaaasplugin.database.entities.MachineUser;
import org.icatproject.topcatdaaasplugin.exceptions.DaaasException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * @author elz24996
 */
@Stateless
@LocalBean
@Path("admin")
public class AdminResource {

    private static final Logger logger = LoggerFactory.getLogger(UserResource.class);

    @EJB
    Database database;

    @EJB
    TopcatClient topcatClient;

    @GET
    @Path("/machines")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getMachines(
            @QueryParam("icatUrl") String icatUrl,
            @QueryParam("sessionId") String sessionId,
            @QueryParam("queryOffset") String queryOffset) {
        try {
            authorize(icatUrl, sessionId);

            StringBuilder query = new StringBuilder();
            query.append("SELECT machine FROM Machine machine ");

            if (queryOffset != null) {
                query.append(queryOffset);
            }

            return database.query(query.toString()).toResponse();
        } catch (DaaasException e) {
            return e.toResponse();
        } catch (Exception e) {
            return new DaaasException(e.getMessage()).toResponse();
        }
    }

    @GET
    @Path("/machines/{id}/screenshot")
    @Produces("image/png")
    public Response getMachineScreenshot(
            @PathParam("id") String id,
            @QueryParam("icatUrl") String icatUrl,
            @QueryParam("sessionId") String sessionId) {
        try {
            authorize(icatUrl, sessionId);

            Map<String, Object> params = new HashMap<>();
            params.put("id", id);

            Machine machine = (Machine) database.query("select machine from Machine machine where machine.id = :id", params).get(0);
            if (machine == null) {
                throw new DaaasException("No such machine.");
            }

            CacheControl cacheControl = new CacheControl();
            cacheControl.setNoStore(true);
            return Response.ok(machine.getScreenshot()).cacheControl(cacheControl).build();
        } catch (DaaasException e) {
            return e.toResponse();
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                message = e.toString();
            }
            return new DaaasException(message).toResponse();
        }
    }

    @GET
    @Path("/machines/{id}/enableAccess")
    @Produces({MediaType.APPLICATION_JSON})
    public Response enableAccessToMachine(
            @PathParam("id") String id,
            @QueryParam("icatUrl") String icatUrl,
            @QueryParam("sessionId") String sessionId) {

        logger.info("Attempting to get access to a machine: {}", id);

        try {
            authorize(icatUrl, sessionId);

            Map<String, Object> params = new HashMap<>();
            params.put("id", id);

            Machine machine = (Machine) database.query("select machine from Machine machine where machine.id = :id", params).get(0);
            if (machine == null) {
                throw new DaaasException("No such machine.");
            }

            IcatClient icatClient = new IcatClient(icatUrl, sessionId);
            String userName = icatClient.getUserName();
            Properties properties = new Properties();

            MachineUser machineUser = new MachineUser();
            machineUser.setUserName(userName);
            machineUser.setType("SECONDARY");
            machineUser.setWebsockifyToken(UUID.randomUUID().toString());
            machineUser.setMachine(machine);
            database.persist(machineUser);

            com.stfc.useroffice.webservice.UserOfficeWebService_Service service = new com.stfc.useroffice.webservice.UserOfficeWebService_Service();
            com.stfc.useroffice.webservice.UserOfficeWebService port = service.getUserOfficeWebServicePort();
            String fedId = port.getPersonDetailsFromUserNumber(properties.getProperty("uokey"), userName.replace("uows/", "")).getFedId();
            SshClient sshClient = new SshClient(machine.getHost());
            sshClient.exec("add_secondary_user " + fedId);
            sshClient.exec("add_websockify_token " + machineUser.getWebsockifyToken());


            return machine.toResponse();
        } catch (DaaasException e) {
            return e.toResponse();
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                message = e.toString();
            }
            return new DaaasException(message).toResponse();
        }
    }

    @GET
    @Path("/machines/{id}/disableAccess")
    @Produces({MediaType.APPLICATION_JSON})
    public Response disableAccessToMachine(
            @PathParam("id") String id,
            @QueryParam("icatUrl") String icatUrl,
            @QueryParam("sessionId") String sessionId) {
        try {
            authorize(icatUrl, sessionId);

            Map<String, Object> params = new HashMap<>();
            params.put("id", id);

            Machine machine = (Machine) database.query("select machine from Machine machine where machine.id = :id", params).get(0);
            if (machine == null) {
                throw new DaaasException("No such machine.");
            }

            IcatClient icatClient = new IcatClient(icatUrl, sessionId);
            String userName = icatClient.getUserName();
            Properties properties = new Properties();

            EntityList<MachineUser> newMachineUsers = new EntityList<>();

            for (MachineUser machineUser : machine.getMachineUsers()) {
                if (machineUser.getType().equals("PRIMARY") || !machineUser.getUserName().equals(userName)) {
                    newMachineUsers.add(machineUser);
                } else {
                    com.stfc.useroffice.webservice.UserOfficeWebService_Service service = new com.stfc.useroffice.webservice.UserOfficeWebService_Service();
                    com.stfc.useroffice.webservice.UserOfficeWebService port = service.getUserOfficeWebServicePort();
                    String fedId = port.getPersonDetailsFromUserNumber(properties.getProperty("uokey"), userName.replace("uows/", "")).getFedId();

                    SshClient sshClient = new SshClient(machine.getHost());
                    sshClient.exec("remove_secondary_user " + fedId);
                    sshClient.exec("remove_websockify_token " + machineUser.getWebsockifyToken());
                }
            }

            machine.setMachineUsers(newMachineUsers);
            database.persist(machine);

            return machine.toResponse();
        } catch (DaaasException e) {
            return e.toResponse();
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                message = e.toString();
            }
            return new DaaasException(message).toResponse();
        }
    }

    private void authorize(String icatUrl, String sessionId) throws Exception {
        if (!topcatClient.isAdmin(icatUrl, sessionId)) {
            throw new DaaasException("You must be a Topcat admin user to do this.");
        }
    }

}
