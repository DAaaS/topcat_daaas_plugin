package org.icatproject.topcatdaaasplugin.cloudclient;

import org.icatproject.topcatdaaasplugin.EntityList;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.json.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


@DependsOn("TrustManagerInstaller")
@Singleton
@Startup
@Stateless
public class CloudClient {

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
        logger.debug("Creating new {} HTTP client", type);
        HttpClient client = null;
        try {
            Properties properties = new Properties();
            createSession();
            if ("COMPUTE".equals(type)) {
                client = new HttpClient(properties.getProperty("computeEndpoint") + "/v2/" + properties.getProperty("project"));
            } else if ("IMAGE".equals(type)) {
                client = new HttpClient(properties.getProperty("imageEndpoint") + "/v2");
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
    //     }
    // }
    public Server createServer(String name, String imageRef, String flavorRef, String availabilityZone, Map<String, String> metadata) throws DaaasException {
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

            JsonObjectBuilder metadataNode = Json.createObjectBuilder();
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                metadataNode.add(entry.getKey(), entry.getValue());
            }
            server.add("metadata", metadataNode);

            server.add("key_name", properties.getProperty("sshKeyPairName"));

            String data = Json.createObjectBuilder().add("server", server).build().toString();
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
    public Server getServer(String id) throws DaaasException {
        try {
            Response response = getHTTPClient("COMPUTE").get("servers/" + id, generateStandardHeaders());
            if (response.getCode() != 200) {
                logger.error("Cloud HTTP request for VM {} failed", id);
                throw new BadRequestException(response.toString());
            }
            JsonObject metadata = parseJson(response.toString()).getJsonObject("server").getJsonObject("metadata");
            Server out = new Server();
            out.setId(id);
            out.setStatus(metadata.getString("AQ_STATUS"));
            out.setHost(metadata.getString("HOSTNAMES"));
            return out;
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

}
