package org.icatproject.topcatdaaasplugin.jsonHandler;

public class GsonMachineType {
    private int id;
    private int cloud_id;
    private String cloud_name;
    private String description;
    private int pool_size;
    private String name;
    private GsonMachineTypeParameters parameters;

    GsonMachineType() {
        // no-args constructor
    }

    public String get_name() {
        return name;
    }
    public String get_description() {
        return description;
    }
    public int get_pool_size() {
        return pool_size;
    }
    public String get_group() {
        return parameters.get_group();
    }
}

class GsonMachineTypeParameters {
    private String group;

    GsonMachineTypeParameters() {
        // no-args constructor
    }

    public String get_group() {
        return group;
    }
}
