/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icatproject.topcatdaaasplugin.rest;

import java.util.List;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.io.*;

import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import javax.ejb.EJB;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.JsonObject;
import javax.json.JsonReader;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.icatproject.topcatdaaasplugin.TopcatClient;
import org.icatproject.topcatdaaasplugin.cloudclient.CloudClient;
import org.icatproject.topcatdaaasplugin.exceptions.DaaasException;
import org.icatproject.topcatdaaasplugin.database.Database;
import org.icatproject.topcatdaaasplugin.database.entities.*;

/**
 *
 * @author elz24996
 */
@Stateless
@LocalBean
@Path("admin")
public class AdminResource {
    
    private static final Logger logger = LoggerFactory.getLogger(UserResource.class);

    @EJB
    CloudClient cloudClient;

    @EJB
    Database database;

    @EJB
    TopcatClient topcatClient;

    @GET
    @Path("/flavors")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getFlavors(
        @QueryParam("icatUrl") String icatUrl,
        @QueryParam("sessionId") String sessionId){
        try {
            authorize(icatUrl, sessionId);

            return cloudClient.getFlavors().toResponse();
        } catch(DaaasException e) {
            return e.toResponse();
        } catch(Exception e) {
            return new DaaasException(e.getMessage()).toResponse();
        }
    }

    @GET
    @Path("/images")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getImages(
        @QueryParam("icatUrl") String icatUrl,
        @QueryParam("sessionId") String sessionId) {
        try {
            authorize(icatUrl, sessionId);

            return cloudClient.getImages().toResponse();
        } catch(DaaasException e) {
            return e.toResponse();
        } catch(Exception e) {
            return new DaaasException(e.getMessage()).toResponse();
        }
    }


    @GET
    @Path("/availabilityZones")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAvailabilityZones(
        @QueryParam("icatUrl") String icatUrl,
        @QueryParam("sessionId") String sessionId) {
        try {
            authorize(icatUrl, sessionId);

            return cloudClient.getAvailabilityZones().toResponse();
        } catch(DaaasException e) {
            return e.toResponse();
        } catch(Exception e) {
            return new DaaasException(e.getMessage()).toResponse();
        }
    }

    @GET
    @Path("/machineTypes")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getMachineTypes(
        @QueryParam("icatUrl") String icatUrl,
        @QueryParam("sessionId") String sessionId) {
        try {
            authorize(icatUrl, sessionId);

            return database.query("select machineType from MachineType machineType").toResponse();
        } catch(DaaasException e) {
            return e.toResponse();
        } catch(Exception e) {
            return new DaaasException(e.getMessage()).toResponse();
        }
    }

    @POST
    @Path("/machineTypes")
    @Produces({MediaType.APPLICATION_JSON})
    public Response createMachineType(
        @FormParam("icatUrl") String icatUrl,
        @FormParam("sessionId") String sessionId,
        @FormParam("json") String json,
        @FormParam("logoData") String logoData) {

        try {
            authorize(icatUrl, sessionId);

            InputStream jsonInputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            JsonReader jsonReader = Json.createReader(jsonInputStream);
            JsonObject jsonObject = jsonReader.readObject();
            jsonReader.close();

            MachineType machineType = new MachineType();

            machineType.setName(jsonObject.getString("name"));
            machineType.setDescription(jsonObject.getString("description"));
            machineType.setImageId(jsonObject.getString("imageId"));
            machineType.setFlavorId(jsonObject.getString("flavorId"));
            machineType.setAvailabilityZone(jsonObject.getString("availabilityZone"));
            machineType.setPoolSize(jsonObject.getInt("poolSize"));
            machineType.setAquilonArchetype(jsonObject.getString("aquilonArchetype"));
            machineType.setAquilonDomain(jsonObject.getString("aquilonDomain"));
            machineType.setAquilonPersonality(jsonObject.getString("aquilonPersonality"));
            machineType.setAquilonSandbox(jsonObject.getString("aquilonSandbox"));
            machineType.setAquilonOSVersion(jsonObject.getString("aquilonOSVersion"));


            List<MachineTypeScope> machineTypeScopes = new ArrayList<MachineTypeScope>();
            for(JsonValue machineTypeScopeValue : jsonObject.getJsonArray("scopes")){
                MachineTypeScope machineTypeScope = new MachineTypeScope();
                machineTypeScope.setMachineType(machineType);
                machineTypeScope.setQuery(((JsonObject) machineTypeScopeValue).getString("query"));
                machineTypeScopes.add(machineTypeScope);
            }
            machineType.setMachineTypeScopes(machineTypeScopes);

            database.persist(machineType);

            return machineType.toResponse();
        } catch(DaaasException e) {
            return e.toResponse();
        } catch(Exception e) {
            return new DaaasException(e.getMessage()).toResponse();
        }
    }

    @PUT
    @Path("/machineTypes/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateMachineType(
        @PathParam("id") Integer id,
        @FormParam("icatUrl") String icatUrl,
        @FormParam("sessionId") String sessionId,
        @FormParam("json") String json) {

        try {
            authorize(icatUrl, sessionId);

            MachineType machineType = (MachineType) database.query("select machineType from MachineType machineType where machineType.id = " + id).get(0);

            InputStream jsonInputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            JsonReader jsonReader = Json.createReader(jsonInputStream);
            JsonObject jsonObject = jsonReader.readObject();
            jsonReader.close();

            machineType.setName(jsonObject.getString("name"));
            machineType.setImageId(jsonObject.getString("imageId"));
            machineType.setFlavorId(jsonObject.getString("flavorId"));
            machineType.setAvailabilityZone(jsonObject.getString("availabilityZone"));
            machineType.setPoolSize(jsonObject.getInt("poolSize"));
            machineType.setAquilonArchetype(jsonObject.getString("aquilonArchetype"));
            machineType.setAquilonDomain(jsonObject.getString("aquilonDomain"));
            machineType.setAquilonPersonality(jsonObject.getString("aquilonPersonality"));
            machineType.setAquilonSandbox(jsonObject.getString("aquilonSandbox"));
            machineType.setAquilonOSVersion(jsonObject.getString("aquilonOSVersion"));
            
            List<MachineTypeScope> machineTypeScopes = new ArrayList<MachineTypeScope>();
            for(JsonValue machineTypeScopeValue : jsonObject.getJsonArray("scopes")){
                MachineTypeScope machineTypeScope = new MachineTypeScope();
                machineTypeScope.setMachineType(machineType);
                machineTypeScope.setQuery(((JsonObject) machineTypeScopeValue).getString("query"));
                machineTypeScopes.add(machineTypeScope);
            }
            machineType.setMachineTypeScopes(machineTypeScopes);

            database.persist(machineType);

            return machineType.toResponse();
        } catch(DaaasException e) {
            return e.toResponse();
        } catch(Exception e) {
            return new DaaasException(e.getMessage()).toResponse();
        }
    }

    @PUT
    @Path("/machineTypes/{id}/logo")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateMachineTypeLogo(
        InputStream body,
        @PathParam("id") Integer id,
        @QueryParam("icatUrl") String icatUrl,
        @QueryParam("sessionId") String sessionId,
        @QueryParam("mimeType") String mimeType) {

        try {
            authorize(icatUrl, sessionId);

            MachineType machineType = (MachineType) database.query("select machineType from MachineType machineType where machineType.id = " + id).get(0);

            machineType.setLogoMimeType(mimeType);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = body.read(data, 0, data.length)) != -1) {
              buffer.write(data, 0, nRead);
            }

            buffer.flush();
            machineType.setLogoData(buffer.toByteArray());

            database.persist(machineType);

            return machineType.toResponse();
        } catch(DaaasException e) {
            return e.toResponse();
        } catch(Exception e) {
            return new DaaasException(e.getMessage()).toResponse();
        }
    }


    @DELETE
    @Path("/machineTypes/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteMachineType(
        @PathParam("id") Integer id,
        @QueryParam("icatUrl") String icatUrl,
        @QueryParam("sessionId") String sessionId) {

        try {
            authorize(icatUrl, sessionId);

            MachineType machineType = (MachineType) database.query("select machineType from MachineType machineType where machineType.id = " + id).get(0);
            database.remove(machineType);
            return Response.ok().build();
        } catch(DaaasException e) {
            return e.toResponse();
        } catch(Exception e) {
            return new DaaasException(e.getMessage()).toResponse();
        }
    }

    private void authorize(String icatUrl, String sessionId) throws Exception {
        if(!topcatClient.isAdmin(icatUrl, sessionId)){
            throw new DaaasException("You must be a Topcat admin user to do this.");
        }
    }
    
}
