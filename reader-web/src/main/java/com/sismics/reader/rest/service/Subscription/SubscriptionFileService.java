package com.sismics.reader.rest.service.Subscription;

import com.google.common.io.ByteStreams;
import com.sismics.reader.core.dao.jpa.FeedSubscriptionDao;
import com.sismics.reader.core.dao.jpa.UserDao;
import com.sismics.reader.core.dao.jpa.criteria.FeedSubscriptionCriteria;
import com.sismics.reader.core.dao.jpa.dto.FeedSubscriptionDto;
import com.sismics.reader.core.event.SubscriptionImportedEvent;
import com.sismics.reader.core.model.context.AppContext;
import com.sismics.reader.core.model.jpa.FeedSubscription;
import com.sismics.reader.core.model.jpa.User;
import com.sismics.reader.core.util.DirectoryUtil;
import com.sismics.reader.rest.service.Authentication.AuthenticationService;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ServerException;
import com.sismics.security.IPrincipal;
import com.sismics.util.MessageUtil;
import com.sun.jersey.multipart.FormDataBodyPart;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import java.io.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SubscriptionFileService {

    private final FeedSubscriptionDao feedSubscriptionDao;
    private final IPrincipal principal;
    private final FeedSubscriptionCriteria feedSubscriptionCriteria;
    private final UserDao userDao;

    public SubscriptionFileService(@Context HttpServletRequest request) {
        feedSubscriptionDao = new FeedSubscriptionDao();
        AuthenticationService authService = new AuthenticationService(request);
        this.principal = authService.getPrincipal();
        this.feedSubscriptionCriteria = new FeedSubscriptionCriteria();
        this.userDao = new UserDao();
    }

    public Response getFaviconFile(String id) throws JSONException {
        // Get the subscription
        final FeedSubscription feedSubscription = feedSubscriptionDao.getFeedSubscription(id, principal.getId());
        if (feedSubscription == null) {
            throw new ClientException("SubscriptionNotFound", MessageFormat.format("Subscription not found: {0}", id));
        }

        // Get the favicon
        File faviconDirectory = DirectoryUtil.getFaviconDirectory();
        File[] matchingFiles = faviconDirectory.listFiles((dir, name) -> name.startsWith(feedSubscription.getFeedId()));
        File file = matchingFiles.length > 0 ?
                matchingFiles[0] :
                new File(getClass().getResource("/image/subscription.png").getFile());
        StreamingOutput stream = os -> ByteStreams.copy(new FileInputStream(file), os);
        return Response.ok(stream)
                .header("Expires", new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z").format(new Date().getTime() + 3600000 * 24 * 7))
                .header("Content-Disposition", MessageFormat.format("attachment; filename=\"{0}\"", file.getName()))
                .build();
    }

    public Document getOpmlDocument() throws JSONException {
        // Create the XML document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ServerException("UnknownError", "Error building export file", e);
        }
        DOMImplementation impl = builder.getDOMImplementation();
        Document opmlDocument = impl.createDocument(null, null, null);
        opmlDocument.setXmlStandalone(true);
        Element opmlElement = opmlDocument.createElement("opml");
        opmlElement.setAttribute("version", "1.0");
        opmlDocument.appendChild(opmlElement);

        // Add head element
        Element headElement = opmlDocument.createElement("head");
        opmlElement.appendChild(headElement);

        // Add title element
        Element titleElement = opmlDocument.createElement("title");
        titleElement.setTextContent(MessageUtil.getMessage(principal.getLocale(), "reader.export.title", principal.getName()));
        headElement.appendChild(titleElement);

        // Add body element
        Element bodyElement = opmlDocument.createElement("body");
        opmlElement.appendChild(bodyElement);

        // Search this user's subscriptions
        feedSubscriptionCriteria.setUserId(principal.getId());
        List<FeedSubscriptionDto> feedSubscriptionList = feedSubscriptionDao.findByCriteria(feedSubscriptionCriteria);

        // Add the categories
        String oldCategoryId = null;
        Element categoryOutlineElement = bodyElement;
        for (FeedSubscriptionDto feedSubscription : feedSubscriptionList) {
            String categoryId = feedSubscription.getCategoryId();

            if (!categoryId.equals(oldCategoryId)) {
                if (feedSubscription.getCategoryParentId() != null) {
                    categoryOutlineElement = opmlDocument.createElement("outline");
                    categoryOutlineElement.setAttribute("title", feedSubscription.getCategoryName());
                    categoryOutlineElement.setAttribute("text", feedSubscription.getCategoryName());
                    bodyElement.appendChild(categoryOutlineElement);
                } else {
                    categoryOutlineElement = bodyElement;
                }
            }
            Element subscriptionOutlineElement = opmlDocument.createElement("outline");
            subscriptionOutlineElement.setAttribute("type", "rss");
            subscriptionOutlineElement.setAttribute("title", feedSubscription.getFeedSubscriptionTitle());
            subscriptionOutlineElement.setAttribute("text", feedSubscription.getFeedSubscriptionTitle());
            subscriptionOutlineElement.setAttribute("xmlUrl", feedSubscription.getFeedRssUrl());
            subscriptionOutlineElement.setAttribute("htmlUrl", feedSubscription.getFeedUrl());
            categoryOutlineElement.appendChild(subscriptionOutlineElement);

            oldCategoryId = categoryId;
        }
        return opmlDocument;
    }

    public JSONObject startFileImport(InputStream in, File importFile) throws IOException {
        IOUtils.copy(in, new FileOutputStream(importFile));
        User user = userDao.getById(principal.getId());
        SubscriptionImportedEvent event = new SubscriptionImportedEvent();
        event.setUser(user);
        event.setImportFile(importFile);
        AppContext.getInstance().getEventBusManager().getImportEventBus().post(event);

        // Always return ok
        return new JSONObject();
    }

    public Response getFileExport() throws JSONException {
        Document opmlDocument = getOpmlDocument();

        Response.ResponseBuilder response = Response.ok();
        final String fileName = "subscriptions.xml";
        response.header("Content-Disposition", MessageFormat.format("attachment; filename=\"{0}\"", fileName));

        DOMSource domSource = new DOMSource(opmlDocument);
        return response.entity(domSource).build();
    }
    public Response importFileEvent(FormDataBodyPart fileBodyPart) throws JSONException {
        InputStream in = fileBodyPart.getValueAs(InputStream.class);
        File importFile = null;
        try {
            // Copy the incoming stream content into a temporary file
            importFile = File.createTempFile("reader_opml_import", null);

            JSONObject response = startFileImport(in, importFile);
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

}
