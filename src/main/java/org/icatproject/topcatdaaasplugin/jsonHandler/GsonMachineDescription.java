package org.icatproject.topcatdaaasplugin.jsonHandler;

public class GsonMachineDescription {
    private int id;
    private String name;
    private String description;
    private String logo;

    GsonMachineDescription() {
        // no-args constructor
    }

    public int get_id() {
        return id;
    }
    public String get_name() {
        return name;
    }
    public String get_description() {
        return description;
    }
    public String get_logo() {
        return logo;
    }
}
