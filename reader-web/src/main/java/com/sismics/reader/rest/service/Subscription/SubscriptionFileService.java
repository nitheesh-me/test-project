package com.sismics.reader.rest.service.Subscription;

import com.sismics.reader.core.dao.jpa.CategoryDao;
import com.sismics.reader.core.dao.jpa.FeedSubscriptionDao;
import com.sismics.reader.core.dao.jpa.UserArticleDao;
import com.sismics.reader.core.dao.jpa.criteria.FeedSubscriptionCriteria;
import com.sismics.reader.core.dao.jpa.criteria.UserArticleCriteria;
import com.sismics.reader.core.dao.jpa.dto.FeedSubscriptionDto;
import com.sismics.reader.core.dao.jpa.dto.UserArticleDto;
import com.sismics.reader.core.model.jpa.Category;
import com.sismics.reader.core.model.jpa.FeedSubscription;
import com.sismics.reader.core.util.DirectoryUtil;
import com.sismics.reader.core.util.jpa.PaginatedList;
import com.sismics.reader.core.util.jpa.PaginatedLists;
import com.sismics.reader.rest.assembler.ArticleAssembler;
import com.sismics.reader.rest.service.Authentication.AuthencticationService;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ServerException;
import com.sismics.rest.util.JsonUtil;
import com.sismics.security.IPrincipal;
import com.sismics.util.MessageUtil;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SubscriptionFileService {

    private final FeedSubscriptionDao feedSubscriptionDao;
    private final IPrincipal principal;
    private final FeedSubscriptionCriteria feedSubscriptionCriteria;
    private final UserArticleCriteria userArticleCriteria;
    private final CategoryDao categoryDao;
    private final UserArticleCriteria afterArticleCriteria;

    public SubscriptionFileService(@Context HttpServletRequest request) {
        feedSubscriptionDao = new FeedSubscriptionDao();
        AuthencticationService authService = new AuthencticationService(request);
        this.principal = authService.getPrincipal();
        this.feedSubscriptionCriteria = new FeedSubscriptionCriteria();
        this.userArticleCriteria = new UserArticleCriteria();
        this.categoryDao = new CategoryDao();
        this.afterArticleCriteria = new UserArticleCriteria();
    }

    public File getFaviconFile(String id) throws JSONException {
        // Get the subscription
        final FeedSubscription feedSubscription = feedSubscriptionDao.getFeedSubscription(id, principal.getId());
        if (feedSubscription == null) {
            throw new ClientException("SubscriptionNotFound", MessageFormat.format("Subscription not found: {0}", id));
        }

        // Get the favicon
        File faviconDirectory = DirectoryUtil.getFaviconDirectory();
        File[] matchingFiles = faviconDirectory.listFiles((dir, name) -> name.startsWith(feedSubscription.getFeedId()));
        final File faviconFile = matchingFiles.length > 0 ?
                matchingFiles[0] :
                new File(getClass().getResource("/image/subscription.png").getFile());
        return faviconFile;
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
    }
