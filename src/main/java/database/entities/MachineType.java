package org.icatproject.topcatdaaasplugin.database.entities;


import java.io.Serializable;
import java.util.Date;

import javax.json.Json;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlRootElement;
import javax.json.JsonObjectBuilder;


import org.icatproject.topcatdaaasplugin.Entity;


@javax.persistence.Entity
@Table(name = "MACHINETYPE")
@XmlRootElement
public class MachineType extends Entity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "PERSONALITY", nullable = false)
    private String personality;

    @Column(name = "IMAGE_ID", nullable = false)
    private String imageId;

    @Column(name = "FLAVOR_ID", nullable = false)
    private String flavorId;

    @Column(name = "POOL_SIZE", nullable = false)
    private Long poolSize;

    @Column(name = "CREATED_AT", nullable=false, updatable=false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPersonality() {
        return personality;
    }

    public void setPersonality(String personality) {
       this.personality = personality;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getFlavorId() {
        return flavorId;
    }

    public void setFlavorId(String flavorId) {
        this.flavorId = flavorId;
    }

    public Long getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(Long poolSize) {
        this.poolSize = poolSize;
    } 

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    private void createAt() {
        this.createdAt = new Date();
    }

    public JsonObjectBuilder toJsonObjectBuilder(){
        JsonObjectBuilder out = Json.createObjectBuilder();
        out.add("id", getId());
        out.add("name", getName());
        out.add("imageId", getImageId());
        out.add("flavorId", getFlavorId());
        out.add("pooSize", getPoolSize());
        out.add("createAt", getCreatedAt().toString());
        return out;
    }

}


