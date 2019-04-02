package org.icatproject.topcatdaaasplugin.vmm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.icatproject.topcatdaaasplugin.Properties;
import org.icatproject.topcatdaaasplugin.database.entities.Machine;
import org.icatproject.topcatdaaasplugin.exceptions.DaaasException;
import org.icatproject.topcatdaaasplugin.httpclient.HttpClient;
import org.icatproject.topcatdaaasplugin.jsonHandler.GsonMachine;
import org.icatproject.topcatdaaasplugin.jsonHandler.GsonMachineType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class VmmClient {
    private static final Logger logger = LoggerFactory.getLogger(VmmClient.class);
    private static final Properties properties = new Properties();
    private Gson gson = new GsonBuilder().serializeNulls().create();
    private HttpClient httpClient;
    private Map<String, String> clientHeaders;

    public VmmClient () {
        httpClient = new HttpClient(properties.getProperty("vmmHost"));
        clientHeaders = get_vmm_client_headers();
    }

    private Map<String, String> get_vmm_client_headers() {
        String vmmUser = properties.getProperty("vmmUser");
        String vmmPassword = properties.getProperty("vmmPassword");
        Map<String, String> headers = new HashMap<>();
        headers.put("VMM-User", vmmUser);
        headers.put("VMM-Password", vmmPassword);
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private GsonMachineType get_machine_type(long machineTypeId) throws DaaasException {
        String machineTypeJson;
        try {
            machineTypeJson = httpClient.get("machinetypes?id="+machineTypeId, clientHeaders).toString();
        } catch (Exception e) {
            logger.debug("getMachineTypes Exception: " + e.getMessage());
            throw new DaaasException(e.getMessage());
        }
        if (machineTypeJson.equals("null")) {
            logger.info("Machine type " + machineTypeId + "not found, I have no idea how.");
            throw new DaaasException("Machine type doesn't seem to exist any more.");
        }

        GsonMachineType gsonMachineType = gson.fromJson(machineTypeJson, GsonMachineType[].class)[0];
        return gsonMachineType;
    }

    public Machine acquire_machine(long machineTypeId) throws DaaasException {
        String machineJson;
        try {
            String params = "{\"machine_type_id\": " + machineTypeId + "}";
            // This request will return "null" when there are no machines available, otherwise json machine
            machineJson = httpClient.post("machines", clientHeaders, params).toString();
        } catch (Exception e) {
            logger.debug("getMachineTypes Exception: " + e.getMessage());
            throw new DaaasException(e.getMessage());
        }
        if (machineJson.equals("null")) {
            logger.info("No machines of type " + machineTypeId + " were available.");
            throw new DaaasException("No more machines of this type are available - please try again later.");
        }
        GsonMachine gsonMachine = gson.fromJson(machineJson, GsonMachine.class);
        Machine machine = new Machine();
        String machineTypeName = get_machine_type(machineTypeId).get_name();
        machine.setName(machineTypeName);
        machine.setId(Integer.toString(gsonMachine.get_id()));
        machine.setHost(gsonMachine.get_hostname());
        if(!gsonMachine.get_state().equals("acquired")) {
            throw new DaaasException("Machine not acquired.");
        }
        return machine;
    }

    public String get_machine_types_json() throws Exception {
        return httpClient.get("machinetypes", clientHeaders).toString();
    }

    public void delete_machine(String id) throws DaaasException {
        int response;
        try {
            // This request will return 200 if the machine is deleted
            response = httpClient.delete("machines?id=" + id, clientHeaders).getCode();
        } catch (Exception e) {
            logger.debug("delete_machine Exception: " + e.getMessage());
            throw new DaaasException(e.getMessage());
        }
        if (response != 200) {
            logger.info("Could not delete machine with ID: " + id + ".");
            throw new DaaasException("Could not delete machine with ID: " + id + ".");
        }
    }
}
