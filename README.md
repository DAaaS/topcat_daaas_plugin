# Archived on 19th October 2021


# Topcat DAaaS (Data Analysis as a Service) Plugin

The front end interface for accessing the Science and Technology Facilities Council's computational resources for remote analysis.

## Installation

1. Do a Bower install:

```
cd topcat_daaas_plugin/src/main/webapp
bower install
```

2. Build the plugin:

```
cd topcat_daaas_plugin/
mvn install
```

3. In the `topcat_daaas_plugin-setup.properties` file ensure that the database and Glassfish properties are configured correctly and in the `topcat_daaas_plugin.properties` file ensure that the OpenStack login information is entered correctly.

4. Install the plugin:

```
./setup install
```

## Configuring TOPCAT

1. In `topcat.json` add the following lines to make the 'My Machines' tab appear:

```
"plugins":[
        "<server_name>/topcat_daaas_plugin"
    ],
    "daaas": {
        "createMachineDelaySeconds": 5
    }
```
	
2. Modify `lang.json` to give the tab the right name. Under ADMIN, add the following:

```
"MAIN_TAB": {
            "MACHINE_TYPES": "Machine Tab"
        }
```
3. Run another setup-install on TOPCAT and refresh your browser.

4. Make everything secure by using https rather than http so that noVNC works correctly.

5. Modify `lang.json` by adding a `DAAAS` JSON where you specify alternative names for associated placeholders. For example:

```
"DAAAS": {
        "MY_MACHINES": "Analysis Environments",
        "CREATE_NEW_MACHINE": "Create Analysis Environments",
        "MACHINE_NOT_AVAILABLE": "No more analysis environments of this type are available - please try again later.",
        "CREATING_MACHINE": "Please wait while your analysis environment gets created."
    }
```




