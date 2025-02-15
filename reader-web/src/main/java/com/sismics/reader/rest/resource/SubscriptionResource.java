package com.sismics.reader.rest.resource;

import com.google.common.io.ByteStreams;
import com.sismics.reader.core.dao.jpa.*;
import com.sismics.reader.core.event.SubscriptionImportedEvent;
import com.sismics.reader.core.model.context.AppContext;
import com.sismics.reader.core.model.jpa.*;
import com.sismics.reader.rest.constant.BaseFunction;
import com.sismics.reader.rest.service.Authentication.AuthencticationService;
import com.sismics.reader.rest.service.Subscription.SubscriptionFeedService;
import com.sismics.reader.rest.service.Subscription.SubscriptionFileService;
import com.sismics.reader.rest.service.Subscription.SubscriptionManagementService;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.exception.ServerException;
import com.sismics.rest.util.ValidationUtil;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataParam;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.transform.dom.DOMSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Feed subscriptions REST resources.
 * 
 * @author jtremeaux
 */
@Path("/subscription")
public class SubscriptionResource {

    private final SubscriptionFileService subscriptionFileService;
    private final SubscriptionManagementService subscriptionManagementService;
    private final AuthencticationService authencticationService;
    private final SubscriptionFeedService subscriptionFeedService;
    private final UserDao userDao;

    public SubscriptionResource(@Context HttpServletRequest request) throws JSONException {
        this.authencticationService = new AuthencticationService(request);
        this.subscriptionManagementService = new SubscriptionManagementService(request);
        this.subscriptionFileService= new SubscriptionFileService(request);
        this.subscriptionFeedService = new SubscriptionFeedService(request);
        this.userDao = new UserDao();
    }

    /**
     * Returns the categories and subscriptions of the current user.
     * 
     * @param unread Returns only subscriptions having unread articles
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(
            @QueryParam("unread") boolean unread) throws JSONException {
        if (!authencticationService.authenticate()) {
            throw new ForbiddenClientException();
        }

        JSONObject response = subscriptionFeedService.getSubsNCategories(unread);
        return Response.ok().entity(response).build();
    }

    /**
     * Returns the subscription informations and paginated articles.
     * 
     * @param id Subscription ID
     * @param unread Returns only unread articles
     * @param limit Page limit
     * @param afterArticle Start the list after this article
     * @return Response
     */
    @GET
    @Path("{id: [a-z0-9\\-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(
            @PathParam("id") String id,
            @QueryParam("unread") boolean unread,
            @QueryParam("limit") Integer limit,
            @QueryParam("after_article") String afterArticle) throws JSONException {
        if (!authencticationService.authenticate()) {
            throw new ForbiddenClientException();
        }

        JSONObject response = subscriptionFeedService.getSubsNPage(id, unread, limit, afterArticle);

        return Response.ok().entity(response).build();
    }


    /**
     * Returns the subscription synchronizations.
     * 
     * @param id Subscription ID
     * @return Response
     */
    @GET
    @Path("{id: [a-z0-9\\-]+}/sync")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSynchronization(
            @PathParam("id") String id) throws JSONException {
        if (!authencticationService.authenticate()) {
            throw new ForbiddenClientException();
        }

        JSONObject response = subscriptionManagementService.syncSubscription(id);

        return Response.ok().entity(response).build();
    }

    /**
     * Adds a subscription to a feed.
     * 
     * @param url URL of a feed, or a web page referencing a feed 
     * @param title Feed title
     * @return Response
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response add(
            @FormParam("url") String url,
            @FormParam("title") String title) throws JSONException {
        if (!authencticationService.authenticate()) {
            throw new ForbiddenClientException();
        }
        
        // Validate input data
        ValidationUtil.validateRequired(url, "url");
        url = ValidationUtil.validateHttpUrl(url, "url");
        title = ValidationUtil.validateLength(title, "title", null, 100, true);

        JSONObject response = subscriptionManagementService.createSubscription(url, title);
        return Response.ok().entity(response).build();
    }

    /**
     * Updates the subscription.
     * 
     * @param id Subscription ID
     * @param title Subscription title (overrides the title set in the RSS feed)
     * @param categoryId Category ID
     * @param order Display order of this subscription in its category
     * @return Response
     */
    @POST
    @Path("{id: [a-z0-9\\-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(
            @PathParam("id") String id,
            @FormParam("title") String title,
            @FormParam("category") String categoryId,
            @FormParam("order") Integer order) throws JSONException {
        if (!authencticationService.authenticate()) {
            throw new ForbiddenClientException();
        }
        
        // Validate input data
        title = ValidationUtil.validateLength(title, "name", 1, 100, true);

        JSONObject response = subscriptionManagementService.updateSubscription(id, title, categoryId, order);
        return Response.ok().entity(response).build();
    }

    /**
     * Returns the favicon of this subscription, or the default favicon.
     * 
     * @param id Subscription ID
     * @return Response
     */
    @GET
    @Path("{id: [a-z0-9\\-]+}/favicon")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response favicon(
            @PathParam("id") String id) throws JSONException {
        if (!authencticationService.authenticate()) {
            throw new ForbiddenClientException();
        }

        final File faviconFile = subscriptionFileService.getFaviconFile(id);

        StreamingOutput stream = os -> ByteStreams.copy(new FileInputStream(faviconFile), os);
        return Response.ok(stream)
                .header("Expires", new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z").format(new Date().getTime() + 3600000 * 24 * 7))
                .header("Content-Disposition", MessageFormat.format("attachment; filename=\"{0}\"", faviconFile.getName()))
                .build();
    }

    /**
     * Marks all articles in this subscription as read.
     * 
     * @param id Subscription ID
     * @return Response
     */
    @POST
    @Path("{id: [a-z0-9\\-]+}/read")
    @Produces(MediaType.APPLICATION_JSON)
    public Response read(
            @PathParam("id") String id) throws JSONException {
        if (!authencticationService.authenticate()) {
            throw new ForbiddenClientException();
        }

        JSONObject response = subscriptionManagementService.markAllRead(id);
        response.put("status", "ok");
        return Response.ok().entity(response).build();
    }

    /**
     * Deletes a subscription.
     * 
     * @param id Subscription ID
     * @return Response
     */
    @DELETE
    @Path("{id: [a-z0-9\\-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(
            @PathParam("id") String id) throws JSONException {
        if (!authencticationService.authenticate()) {
            throw new ForbiddenClientException();
        }

        JSONObject response = subscriptionManagementService.deleteSubscription(id);
        return Response.ok().entity(response).build();
    }

    /**
     * Imports some data into the user's account.
     * The content of the file to import must be PUT as multipart/form-data.
     * The file can be either a OPML file, or a ZIP containing an OPML file and some Google Takeout data.
     * 
     * @param fileBodyPart File to import
     * @return Response
     */
    @PUT
    @Consumes("multipart/form-data") 
    @Path("import")
    public Response importFile(
            @FormDataParam("file") FormDataBodyPart fileBodyPart) throws JSONException {

        authencticationService.checkBaseFunction(BaseFunction.IMPORT);
        
        // Validate input data
        ValidationUtil.validateRequired(fileBodyPart, "file");

        User user = userDao.getById(authencticationService.getPrincipal().getId());
        
        InputStream in = fileBodyPart.getValueAs(InputStream.class);
        File importFile = null;
        try {
            // Copy the incoming stream content into a temporary file
            importFile = File.createTempFile("reader_opml_import", null);
            IOUtils.copy(in, new FileOutputStream(importFile));
            
            SubscriptionImportedEvent event = new SubscriptionImportedEvent();
            event.setUser(user);
            event.setImportFile(importFile);
            AppContext.getInstance().getEventBusManager().getImportEventBus().post(event);

            // Always return ok
            JSONObject response = new JSONObject();
            response.put("status", "ok");
            return Response.ok().entity(response).build();
        } catch (Exception e) {
            if (importFile != null) {
                try {
                    importFile.delete();
                } catch (SecurityException e2) {
                    // NOP
                }
            }
            throw new ServerException("ImportError", "Error importing OPML file", e);
        }
    }

    /**
     * Exports all the user's feeds to an OPML file.
     * 
     * @return Response
     */
    @GET
    @Path("export")
    @Produces(MediaType.APPLICATION_XML)
    public Response export() throws JSONException {
        if (!authencticationService.authenticate()) {
            throw new ForbiddenClientException();
        }

        Document opmlDocument = subscriptionFileService.getOpmlDocument();

        ResponseBuilder response = Response.ok();
        final String fileName = "subscriptions.xml";
        response.header("Content-Disposition", MessageFormat.format("attachment; filename=\"{0}\"", fileName));

        DOMSource domSource = new DOMSource(opmlDocument);
        return response.entity(domSource).build();
    }

}
