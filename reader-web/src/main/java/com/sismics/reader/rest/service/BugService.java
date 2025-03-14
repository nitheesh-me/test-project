package com.sismics.reader.rest.service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import com.sismics.reader.core.reporting.Bug;
import com.sismics.reader.core.reporting.BugReportManager;

@Path("/bugs")
public class BugService {
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Bug> getAllBugs() {
        return BugReportManager.getInstance().getBugs();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createBug(@FormParam("description") String description) {
        System.out.println("Creating bug: " + description);
        BugReportManager.getInstance().reportBug(description);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteBug(@PathParam("id") String id) {
        BugReportManager.getInstance().deleteBug(id);
        return Response.ok().build();
    }

    @POST
    @Path("/{id}/resolve")
    public Response resolveBug(@PathParam("id") String id) {
        BugReportManager manager = BugReportManager.getInstance();
        manager.resolveBug(id);
        return Response.ok().build();
    }
}