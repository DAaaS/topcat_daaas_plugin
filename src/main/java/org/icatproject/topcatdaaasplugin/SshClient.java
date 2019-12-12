package org.icatproject.topcatdaaasplugin;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public class SshClient {
    private static final Logger logger = LoggerFactory.getLogger(SshClient.class);

    private String host;

    public SshClient(String host) {
        this.host = host;
    }

    public String exec(String commandToRun) throws IOException, InterruptedException {
        //logger.trace("exec " + host + " :" + commandToRun);


        Properties properties = new Properties();
        String sshPrivateKeyFile = properties.getProperty("sshPrivateKeyFile");
        String sshUsername = properties.getProperty("sshUsername");

        String[] command = new String[]{
                "/usr/bin/ssh", "-t", sshUsername + "@" + host,
                "-i", sshPrivateKeyFile,
                "-o", "StrictHostKeyChecking no",
                "-o", "UserKnownHostsFile /dev/null",
                "-o", "PreferredAuthentications publickey",
                "-o", "ConnectTimeout 3",
                commandToRun
        };

        String cmdout = "";
        for (String s : command) {
            cmdout += " " + s;
        }
        logger.trace("Running command :" + cmdout);


        Process process = Runtime.getRuntime().exec(command);

        logger.trace("Finished running command");

        String out = readInputStream(process.getInputStream());
        String error = readInputStream(process.getErrorStream());

        //logger.trace("Out-> " + out);
        logger.trace("Error-> " + error);

        //process.waitFor();


        logger.trace("Returning...");
        return out;
    }

    private String readInputStream(InputStream inputStream) throws IOException {
        StringBuffer out = new StringBuffer();
        BufferedInputStream in = new BufferedInputStream(inputStream);
        byte[] bytes = new byte[1];
        while (in.read(bytes) != -1) {
            out.append(new String(bytes, StandardCharsets.US_ASCII));
        }
        return out.toString();
    }

}



