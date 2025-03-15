package com.sismics.reader.rest.resource;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import com.sismics.reader.core.dao.BugDao;
import com.sismics.reader.core.dao.BugDto;
import com.sismics.reader.core.reporting.BugReportManager;
import java.util.List;
import java.util.logging.Logger;

/**
 * Bug reporting REST resource.
 */
@Path("/bugs")
public class BugResource {
    private static final Logger LOGGER = Logger.getLogger(BugResource.class.getName());
    private final HttpServletRequest request;

    public BugResource(@Context HttpServletRequest request) {
        this.request = request;
    }

    /**
     * GET /bugs - Returns list of bugs as JSON.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBugs() {
        try {
            List<BugDto> bugs = BugDao.getAllBugs();
            return Response.ok(bugs).build();
        } catch (Exception ex) {
            LOGGER.severe("Error in getBugs: " + ex.getMessage());
            return Response.serverError().entity("Error: " + ex.getMessage()).build();
        }
    }

    /**
     * POST /bugs - Creates a new bug report.
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createBug(@FormParam("description") String description) {
        try {
            // Create the bug report
            BugReportManager.getInstance().reportBug(description);
            List<BugDto> bugs = BugDao.getAllBugs();
            return Response.ok(bugs).build();
        } catch (Exception ex) {
            LOGGER.severe("Error in createBug: " + ex.getMessage());
            return Response.serverError().entity("Error: " + ex.getMessage()).build();
        }
    }

    /**
     * DELETE /bugs/{id} - Deletes a bug report.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteBug(@PathParam("id") String id) {
        try {
            BugReportManager.getInstance().deleteBug(id);
            List<BugDto> bugs = BugDao.getAllBugs();
            return Response.ok(bugs).build();
        } catch (Exception ex) {
            LOGGER.severe("Error in deleteBug: " + ex.getMessage());
            return Response.serverError().entity("Error: " + ex.getMessage()).build();
        }
    }

    /**
     * POST /bugs/{id}/resolve - Marks a bug as resolved.
     */
    @POST
    @Path("/{id}/resolve")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resolveBug(@PathParam("id") String id) {
        try {
            BugReportManager.getInstance().resolveBug(id);
            List<BugDto> bugs = BugDao.getAllBugs();
            return Response.ok(bugs).build();
        } catch (Exception ex) {
            LOGGER.severe("Error in resolveBug: " + ex.getMessage());
            return Response.serverError().entity("Error: " + ex.getMessage()).build();
        }
    }
}