package org.icatproject.topcatdaaasplugin.cloudclient;

import org.icatproject.topcatdaaasplugin.EntityList;
import org.icatproject.topcatdaaasplugin.Entity;
import org.icatproject.topcatdaaasplugin.Properties;
import org.icatproject.topcatdaaasplugin.cloudclient.entities.AvailabilityZone;
import org.icatproject.topcatdaaasplugin.cloudclient.entities.Flavor;
import org.icatproject.topcatdaaasplugin.cloudclient.entities.Image;
import org.icatproject.topcatdaaasplugin.cloudclient.entities.Server;
import org.icatproject.topcatdaaasplugin.exceptions.BadRequestException;
import org.icatproject.topcatdaaasplugin.exceptions.DaaasException;
import org.icatproject.topcatdaaasplugin.exceptions.UnexpectedException;
import org.icatproject.topcatdaaasplugin.httpclient.HttpClient;
import org.icatproject.topcatdaaasplugin.httpclient.Response;
import org.icatproject.topcatdaaasplugin.database.Database;
import org.icatproject.topcatdaaasplugin.database.entities.Machine;
import org.icatproject.topcatdaaasplugin.database.entities.MachineType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.*;
import javax.json.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.net.InetAddress;


@DependsOn("TrustManagerInstaller")
@Singleton
@Startup
@Stateless
public class CloudClient {

    @EJB
    Database database;

    public enum STATE {
        VACANT, PREPARING, ACQUIRED, FAILED, DELETED;
    }

    private static final Logger logger = LoggerFactory.getLogger(CloudClient.class);

    private String authToken;

    private void createSession() throws Exception {
        try {
            Properties properties = new Properties();

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Content-Type", "application/json");

            // {
            //     "auth" => {
            //         "identity" => {
            //             "methods" => [
            //                 "password"
            //             ],
            //             "password" => {
            //                 "user" => {
            //                     "name" => "elz24996",
            //                     "domain" => {
            //                         "name" => "stfc"
            //                     },
            //                     "password" => "saadsad"
            //                 }
            //             }
            //         },
            //         "scope" => {
            //             "project" => {
            //                 "id" => "3242342342344ada"
            //             }
            //         }
            //     }
            // }

            JsonObjectBuilder auth = Json.createObjectBuilder();
            JsonObjectBuilder identity = Json.createObjectBuilder();
            JsonArrayBuilder methods = Json.createArrayBuilder();
            methods.add("password");
            identity.add("methods", methods);
            JsonObjectBuilder password = Json.createObjectBuilder();
            JsonObjectBuilder user = Json.createObjectBuilder();
            user.add("name", properties.getProperty("username"));
            JsonObjectBuilder domain = Json.createObjectBuilder();
            domain.add("name", properties.getProperty("domain"));
            user.add("domain", domain);
            user.add("password", properties.getProperty("password"));
            password.add("user", user);
            identity.add("password", password);
            auth.add("identity", identity);
            JsonObjectBuilder scope = Json.createObjectBuilder();
            JsonObjectBuilder project = Json.createObjectBuilder();
            project.add("id", properties.getProperty("project"));
            scope.add("project", project);
            auth.add("scope", scope);

            String data = Json.createObjectBuilder().add("auth", auth).build().toString();
            HttpClient identityHttpClient = new HttpClient(properties.getProperty("identityEndpoint") + "/v3");
            this.authToken = identityHttpClient.post("auth/tokens", headers, data).getHeader("X-Subject-Token");

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private HttpClient getHTTPClient(String type) {
        HttpClient client = null;
        try {
            Properties properties = new Properties();
            createSession();
            if ("COMPUTE".equals(type)) {
                client = new HttpClient(properties.getProperty("computeEndpoint") + "/v2/" + properties.getProperty("project"));
            } else if ("IMAGE".equals(type)) {
                client = new HttpClient(properties.getProperty("imageEndpoint") + "/v2");
            } else if ("NETWORK".equals(type)) {
                client = new HttpClient(properties.getProperty("networkEndpoint") + "/v2.0");
            } else {
                throw new IllegalStateException("Unknown client type: " + type);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return client;
    }

    public EntityList<Flavor> getFlavors() throws DaaasException {
        EntityList<Flavor> out = new EntityList<Flavor>();

        try {
            Response response = getHTTPClient("COMPUTE").get("flavors/detail", generateStandardHeaders());
            if (response.getCode() != 200) {
                throw new BadRequestException(response.toString());
            }
            JsonObject results = parseJson(response.toString());
            for (JsonValue flavorValue : results.getJsonArray("flavors")) {
                JsonObject cloudFlavor = (JsonObject) flavorValue;
                Flavor flavor = new Flavor();
                flavor.setId(cloudFlavor.getString("id"));
                flavor.setName(cloudFlavor.getString("name"));
                flavor.setCpus(cloudFlavor.getInt("vcpus"));
                flavor.setRam(cloudFlavor.getInt("ram"));
                flavor.setDisk(cloudFlavor.getInt("disk"));
                out.add(flavor);
            }
        } catch (Exception e) {
            logger.error("Failed to get list of flavors");
            throw new UnexpectedException(e.getMessage());
        }

        return out;
    }


    public EntityList<Image> getImages() throws DaaasException {
        EntityList<Image> out = new EntityList<Image>();
        Response response = null;

        try {
            response = getHTTPClient("IMAGE").get("images", generateStandardHeaders());
            if (response.getCode() != 200) {
                throw new BadRequestException(response.toString());
            }
            JsonObject results = parseJson(response.toString());
            for (JsonValue imageValue : results.getJsonArray("images")) {
                JsonObject cloudImage = (JsonObject) imageValue;
                Image image = new Image();
                image.setId(cloudImage.getString("id"));
                image.setName(cloudImage.getString("name"));
                image.setSize(cloudImage.getInt("size", 0));
                out.add(image);
            }
        } catch (Exception e) {
            logger.error("Failed to get list of images");
            logger.error(response.toString());
            throw new UnexpectedException(e.getMessage());
        }

        return out;
    }

    public EntityList<AvailabilityZone> getAvailabilityZones() throws DaaasException {
        EntityList<AvailabilityZone> out = new EntityList<AvailabilityZone>();

        try {
            Response response = getHTTPClient("COMPUTE").get("os-availability-zone", generateStandardHeaders());
            if (response.getCode() != 200) {
                throw new BadRequestException(response.toString());
            }
            JsonObject results = parseJson(response.toString());
            for (JsonValue availabilityZoneInfoValue : results.getJsonArray("availabilityZoneInfo")) {
                JsonObject availabilityZoneInfo = (JsonObject) availabilityZoneInfoValue;
                AvailabilityZone availabilityZone = new AvailabilityZone();
                availabilityZone.setName(availabilityZoneInfo.getString("zoneName"));
                availabilityZone.setIsAvailable(availabilityZoneInfo.getJsonObject("zoneState").getBoolean("available"));
                out.add(availabilityZone);
            }
        } catch (Exception e) {
            logger.error("Failed to get availability zones");
            throw new UnexpectedException(e.getMessage());
        }

        return out;
    }


    // {
    //     "server" => {
    //         "name" => "auto-allocate-network",
    //         "imageRef" => "ba123970-efbd-4a91-885a-b069e03e003d",
    //         "flavorRef" => "8a34f302-4cdc-459c-9e45-c5655c94382f",
    //         "availability_zone" => "ceph"
    //         "metadata" => {
    //             "AQ_ARCHETYPE" => "ral-tier1",
    //             "AQ_DOMAIN" => "",
    //             "AQ_PERSONALITY" => "daaas-common",
    //             "AQ_SANDBOX" => "sap86629/daas-excitations",
    //             "AQ_OSVERSION" => "7x-x86_6"
    //         }
    //        "security_groups": [
    //             {
    //                 "name": "default"
    //             }
    //        ],
    //     }
    // }
    public Server createServer(String name, String imageRef, String flavorRef, String availabilityZone) throws DaaasException {
        try {
            logger.info("createServer: " + name + ", " + imageRef + ", " + flavorRef + ", " + availabilityZone);

            Properties properties = new Properties();

            JsonObjectBuilder server = Json.createObjectBuilder();
            server.add("name", name);
            server.add("imageRef", imageRef);
            server.add("flavorRef", flavorRef);
            server.add("availability_zone", availabilityZone);

            JsonArrayBuilder networkList = Json.createArrayBuilder();
            JsonObjectBuilder network = Json.createObjectBuilder();
            network.add("uuid", properties.getProperty("networkId"));
            networkList.add(network);
            server.add("networks", networkList);

            server.add("key_name", properties.getProperty("sshKeyPairName"));

            String[] security_groups = properties.getProperty("security_groups").split(",");
            JsonArrayBuilder securitygroupList = Json.createArrayBuilder();
            for (int i=0; i < security_groups.length; i++) {
                JsonObjectBuilder securitygroup = Json.createObjectBuilder();
                securitygroup.add("name", security_groups[i]);
                securitygroupList.add(securitygroup);
                logger.debug("Adding security group: " + security_groups[i]);
            }
            server.add("security_groups", securitygroupList);

            String data = Json.createObjectBuilder().add("server", server).build().toString();
            logger.debug("Create VM data: " + data);
            Response response = getHTTPClient("COMPUTE").post("servers", generateStandardHeaders(), data);

            if (response.getCode() >= 400) {
                throw new BadRequestException(response.toString());
            }

            Server out = new Server();
            out.setId(parseJson(response.toString()).getJsonObject("server").getString("id"));
            return out;
        } catch (Exception e) {
            logger.error("Failed to create new VM");
            throw new UnexpectedException(e.getMessage());
        }
    }

    /**
     * Get and store metadata associated to the cloud VM.
     */
    public JsonObject getServer(String id) throws DaaasException {
        try {
            Response response = getHTTPClient("COMPUTE").get("servers/" + id, generateStandardHeaders());
            if (response.getCode() != 200) {
                logger.error("Cloud HTTP request for VM {} failed", id);
                throw new BadRequestException(response.toString());
            }
            return parseJson(response.toString()).getJsonObject("server");
        } catch (Exception e) {
            logger.error("Failed to get VM information for {}", id);
            throw new UnexpectedException(e.getMessage());
        }
    }

    public void deleteServer(String id) throws DaaasException {
        try {
            logger.debug("Sending delete request to OpenStack for VM {}", id);
            Response response = getHTTPClient("COMPUTE").delete("servers/" + id, generateStandardHeaders());
            if (response.getCode() >= 400) {
                throw new BadRequestException(response.toString());
            }
            logger.debug("Successfully deleted VM {} from OpenStack", id);
        } catch (Exception e) {
            logger.error("Failed to delete VM {}", id);
            throw new UnexpectedException(e.getMessage());
        }
    }

    private Map<String, String> generateStandardHeaders() {
        Map<String, String> out = new HashMap<String, String>();
        out.put("Content-Type", "application/json");
        out.put("X-Auth-Token", authToken);
        return out;
    }

    private JsonObject parseJson(String json) throws Exception {
        InputStream jsonInputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonReader jsonReader = Json.createReader(jsonInputStream);
        JsonObject out = jsonReader.readObject();
        jsonReader.close();
        return out;
    }

    public String assign_floating_ip(String vm_id) throws DaaasException {
        try {
            Response response = getHTTPClient("NETWORK").get("floatingips?status=DOWN", generateStandardHeaders());
            if (response.getCode() != 200) {
                throw new BadRequestException(response.toString());
            }

            String ip_address_id = null;
            String ip_address = null;
            for (int i = 0; i < parseJson(response.toString()).getJsonArray("floatingips").size(); i++) {
                // first get hold of a unused floating ip address
                ip_address_id = parseJson(response.toString()).getJsonArray("floatingips").getJsonObject(i).getString("id");
                ip_address = parseJson(response.toString()).getJsonArray("floatingips").getJsonObject(i).getString("floating_ip_address");
    
                logger.debug("ipaddress = " + ip_address);

                // we have to manually check if the ip is already in use as openstack give completely unreliable information
                String hostname = InetAddress.getByName(ip_address).getHostName();

                logger.debug("hostname = " + hostname);
    
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("state1", STATE.PREPARING.name());
                params.put("state2", STATE.VACANT.name());
                params.put("state3", STATE.ACQUIRED.name());
                params.put("hostname", hostname);
                EntityList<Entity> hostname_check = database.query("select machine from Machine machine, machine.machineType as machineType where (machine.state = :state1 or machine.state = :state2 or machine.state = :state3) and machine.host = :hostname", params);
 
                if (hostname_check.size() != 0) {
                    logger.warn("Openstack is trying to assign an ip address that is already in use ... again");
                } else {
                    break;
                }
            }

            logger.debug("ipaddress2 = " + ip_address);

            // next get hold of the port id for the VM
            response = getHTTPClient("NETWORK").get("ports?device_id=" + vm_id, generateStandardHeaders());
            if (response.getCode() != 200) {
                throw new BadRequestException(response.toString());
            }

            logger.debug(response.toString());


            String port_id = parseJson(response.toString()).getJsonArray("ports").getJsonObject(0).getString("id");

            // finally associate the port id to the floating ip address
            JsonObjectBuilder port = Json.createObjectBuilder();
            port.add("port_id", port_id);

            JsonObjectBuilder data = Json.createObjectBuilder();
            data.add("floatingip", port);

            response = getHTTPClient("NETWORK").put("floatingips/" + ip_address_id, generateStandardHeaders(), data.build().toString());

            logger.debug(response.toString());

            if (response.getCode() != 200) {
                throw new BadRequestException(response.toString());
            }

            return ip_address;
        } catch (Exception e) {
            logger.error("Failed to assign floating IP");
            throw new UnexpectedException(e.getMessage());
        }
    }

}
