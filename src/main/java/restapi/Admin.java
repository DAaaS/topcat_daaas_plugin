/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icatproject.topcatdaaasplugin.restapi;

import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import javax.ejb.EJB;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.icatproject.topcatdaaasplugin.CloudClient;
import org.icatproject.topcatdaaasplugin.exceptions.DaaasException;

/**
 *
 * @author elz24996
 */
@Stateless
@LocalBean
@Path("admin")
public class RestApi {
    
    private static final Logger logger = LoggerFactory.getLogger(RestApi.class);

    @EJB
    CloudClient cloudClient;

    @GET
    @Path("/machines")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getMachines() {
        try {
            return cloudClient.getMachines().toResponse();
        } catch(DaaasException e) {
            return e.toResponse();
        }
    }
    
    // @GET
    // @Path("/user")
    // @Produces({MediaType.APPLICATION_JSON})
    // public Response getUser(
    //         @QueryParam("sessionId") String sessionId,
    //         @QueryParam("userId") String userId) {
        
    //     try {
    //         if(userId != null){
    //             return new CloudClient(sessionId).getUser(Integer.parseInt(userId)).toResponse();
    //         } else {
    //             return new CloudClient(sessionId).getUser().toResponse();
    //         }
    //     } catch(DaaasException e) {
    //         return e.toResponse();
    //     }
    
    
    @POST
    @Path("/machines")
    @Produces({MediaType.APPLICATION_JSON})
    public Response createMachine(
            @FormParam("templateId") String templateId,
            @FormParam("name") String name) {
        
        try {
            return cloudClient.createMachine(templateId, name).toResponse();
        } catch(DaaasException e) {
            return e.toResponse();
        }
        
    }
    
    @DELETE
    @Path("/machines/{machineId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteMachine(
            @PathParam("machineId") String machineId) {
        try {
            return cloudClient.deleteMachine(machineId).toResponse();
        } catch(DaaasException e) {
            return e.toResponse();
        }
    }
    
    
    @GET
    @Path("/templates")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getTemplates(
            @QueryParam("sessionId") String sessionId) {
        
        try {
            return cloudClient.getTemplates().toResponse();
        } catch(DaaasException e) {
            return e.toResponse();
        }
        
    }

    @GET
    @Path("/machineTypes")
    @Produces({MediaType.APPLICATION_JSON})
    public Response machineTypes(
            @QueryParam("sessionId") String sessionId) {
        
        try {
            
            
            return cloudClient.getTemplates().toResponse();
        } catch(DaaasException e) {
            return e.toResponse();
        }
        
    }

    @GET
    @Path("/test")
    @Produces({MediaType.TEXT_PLAIN})
    public Response test(){
        try {
            return Response.ok().entity(new SshClient("glassfish", "topcat-dev.esc.rl.ac.uk", "/home/vagrant/id_rsa").exec("ping -c 1 google.com")).build();
        } catch(Exception e) {
            return Response.status(400).entity(e.getMessage()).build();
        }
    }

    // private String getUsername(String sessionId){
        
    // }
}