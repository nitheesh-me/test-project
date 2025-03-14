package com.sismics.reader.rest.service;

import com.sismics.reader.core.reporting.Bug;
import com.sismics.reader.core.reporting.BugReportManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/bugs")
public class BugResource {

    /**
     * GET /bugs - Returns a JSON list of bugs.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBugs() {
        List<Bug> bugs = BugReportManager.getInstance().getBugs();
        return Response.ok(bugs).build();
    }

    /**
     * POST /bugs - Creates a new bug report and returns the updated list.
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createBug(@FormParam("description") String description) {
        BugReportManager.getInstance().reportBug(description);
        List<Bug> bugs = BugReportManager.getInstance().getBugs();
        return Response.ok(bugs).build();
    }

    /**
     * DELETE /bugs/{id} - Deletes a bug and returns the updated list.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteBug(@PathParam("id") String id) {
        BugReportManager.getInstance().deleteBug(id);
        List<Bug> bugs = BugReportManager.getInstance().getBugs();
        return Response.ok(bugs).build();
    }

    /**
     * POST /bugs/{id}/resolve - Marks a bug as resolved and returns the updated list.
     */
    @POST
    @Path("/{id}/resolve")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resolveBug(@PathParam("id") String id) {
        BugReportManager.getInstance().resolveBug(id);
        List<Bug> bugs = BugReportManager.getInstance().getBugs();
        return Response.ok(bugs).build();
    }
}