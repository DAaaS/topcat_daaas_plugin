/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Images
 * and open the template in the editor.
 */
package org.icatproject.topcatdaaasplugin.database.entities;

import org.icatproject.topcatdaaasplugin.Entity;
import org.icatproject.topcatdaaasplugin.EntityList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * @author elz24996
 */

@javax.persistence.Entity
@Table(name = "MACHINE")
@XmlRootElement
public class Machine extends Entity {
    private static final Logger logger = LoggerFactory.getLogger(Machine.class);

    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @Id
    @Column(name = "ID", nullable = false)
    private String id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "HOST")
    private String host;

    @Lob
    @Column(name = "SCREENSHOT")
    private byte[] screenshot;

    @Column(name = "CREATED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "machine", orphanRemoval = true)
    private List<MachineUser> machineUsers;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public byte[] getScreenshot() {
        return screenshot;
    }

    public void setScreenshot(byte[] screenshot) {
        this.screenshot = screenshot;
    }

    public String getScreenshotMd5() {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] md5 = messageDigest.digest(getScreenshot());
            return Base64.getEncoder().encodeToString(md5);
        } catch (Exception e) {
            return "";
        }
    }

    @PrePersist
    private void initCreatedAt() {
        this.createdAt = new Date();
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public EntityList<MachineUser> getMachineUsers() {
        EntityList<MachineUser> out = new EntityList<MachineUser>();
        for (MachineUser machineUser : machineUsers) {
            out.add(machineUser);
        }
        return out;
    }

    public void setMachineUsers(List<MachineUser> machineUsers) {
        this.machineUsers = machineUsers;
    }

    public MachineUser getPrimaryUser() {
        for (MachineUser machineUser : getMachineUsers()) {
            if (machineUser.getType().equals("PRIMARY")) {
                return machineUser;
            }
        }
        return null;
    }

    public JsonObjectBuilder toJsonObjectBuilder() {
        JsonObjectBuilder out = Json.createObjectBuilder();
        out.add("id", getId());
        out.add("name", getName());
        out.add("host", getHost());
        out.add("screenshotMd5", getScreenshotMd5());
        if (getCreatedAt() != null) {
            out.add("createdAt", dateFormat.format(getCreatedAt()));
        }
        out.add("users", getMachineUsers().toJsonArrayBuilder());
        return out;
    }

}
