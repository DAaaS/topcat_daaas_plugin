package org.icatproject.topcatdaaasplugin.jsonHandler;

import com.google.gson.Gson;

public class GsonMachine {
    private String provider_id;
    private String state;
    private int machine_type_id;
    private String hostname;

    GsonMachine() {
        // no-args constructor
    }

    public String get_id() {
        return provider_id;
    }
    public String get_state() {
        return state;
    }
    public int get_machine_type_id() {
        return machine_type_id;
    }
    public String get_hostname() {
        return hostname;
    }
}
