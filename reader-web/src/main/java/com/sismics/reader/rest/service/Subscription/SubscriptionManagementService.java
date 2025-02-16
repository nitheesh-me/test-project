package com.sismics.reader.rest.service.Subscription;

import com.sismics.reader.core.dao.jpa.*;
import com.sismics.reader.core.dao.jpa.criteria.FeedSubscriptionCriteria;
import com.sismics.reader.core.dao.jpa.criteria.UserArticleCriteria;
import com.sismics.reader.core.dao.jpa.dto.FeedSubscriptionDto;
import com.sismics.reader.core.model.context.AppContext;
import com.sismics.reader.core.model.jpa.Category;
import com.sismics.reader.core.model.jpa.Feed;
import com.sismics.reader.core.model.jpa.FeedSubscription;
import com.sismics.reader.core.model.jpa.FeedSynchronization;
import com.sismics.reader.core.service.FeedService;
import com.sismics.reader.core.util.EntityManagerUtil;
import com.sismics.reader.rest.service.Authentication.AuthenticationService;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ServerException;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionManagementService
{

    private final FeedSubscriptionDao feedSubscriptionDao;
    private final CategoryDao categoryDao;
    private final FeedSubscriptionCriteria feedSubscriptionCriteria;
    private final FeedSubscription feedSubscription;
    private final AuthenticationService authenticationService;

    public SubscriptionManagementService(@Context HttpServletRequest request) {
         this.feedSubscriptionDao = new FeedSubscriptionDao();
         this.categoryDao = new CategoryDao();
         this.feedSubscriptionCriteria = new FeedSubscriptionCriteria();
         this.feedSubscription = new FeedSubscription();
         this.authenticationService = new AuthenticationService(request);
    }

     public JSONObject updateSubscription(String id, String title, String categoryId, Integer order) throws JSONException {
        // Get the subscription
        FeedSubscription feedSubscription = feedSubscriptionDao.getFeedSubscription(id, authenticationService.getPrincipal().getId());
        if (feedSubscription == null) {
            throw new ClientException("SubscriptionNotFound", MessageFormat.format("Subscription not found: {0}", id));
        }

        // Update the subscription
        if (StringUtils.isNotBlank(title)) {
            feedSubscription.setTitle(title);
        }
        if (StringUtils.isNotBlank(categoryId)) {

            try {
                categoryDao.getCategory(categoryId, authenticationService.getPrincipal().getId());
            } catch (NoResultException e) {
                throw new ClientException("CategoryNotFound", MessageFormat.format("Category not found: {0}", categoryId));
            }

            feedSubscription.setCategoryId(categoryId);
        }
        feedSubscriptionDao.update(feedSubscription);

        // Reorder categories
        if (order != null) {
            feedSubscriptionDao.reorder(feedSubscription, order);
        }

        // Always return ok
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return response;
    }


     public JSONObject createSubscription(String url, String title) throws JSONException {
        // Check if the user is already subscribed to this feed
        feedSubscriptionCriteria.setUserId(authenticationService.getPrincipal().getId())
                .setFeedUrl(url);
        List<FeedSubscriptionDto> feedSubscriptionList = feedSubscriptionDao.findByCriteria(feedSubscriptionCriteria);
        if (!feedSubscriptionList.isEmpty()) {
            throw new ClientException("AlreadySubscribed", "You are already subscribed to this URL");
        }

        // Get feed and articles
        Feed feed;
        final FeedService feedService = AppContext.getInstance().getFeedService();
        try {
            feed = feedService.synchronize(url);
        } catch (Exception e) {
            throw new ServerException("FeedError", MessageFormat.format("Error retrieving feed at {0}", url), e);
            // TODO NoFeedFound if it isn't a feed or a page referencing a feed
        }

        // Check again that we are not subscribed, in case the page URL was replaced by the feed URL
        feedSubscriptionCriteria.setUserId(authenticationService.getPrincipal().getId())
                .setFeedUrl(feed.getRssUrl());
        feedSubscriptionList = feedSubscriptionDao.findByCriteria(feedSubscriptionCriteria);
        if (!feedSubscriptionList.isEmpty()) {
            throw new ClientException("AlreadySubscribed", "You are already subscribed to this URL");
        }

        // Get the root category
        Category category = categoryDao.getRootCategory(authenticationService.getPrincipal().getId());

        // Get the display order
        Integer displayOrder = feedSubscriptionDao.getCategoryCount(category.getId(), authenticationService.getPrincipal().getId());

        // Create the subscription
        feedSubscription.setUserId(authenticationService.getPrincipal().getId());
        feedSubscription.setFeedId(feed.getId());
        feedSubscription.setCategoryId(category.getId());
        feedSubscription.setOrder(displayOrder);
        feedSubscription.setUnreadCount(0);
        feedSubscription.setTitle(title);
        String feedSubscriptionId = feedSubscriptionDao.create(feedSubscription);

        // Create the initial article subscriptions for this user
        EntityManagerUtil.flush();
        feedService.createInitialUserArticle(authenticationService.getPrincipal().getId(), feedSubscription);

        JSONObject response = new JSONObject();
        response.put("id", feedSubscriptionId);
        return response;
    }


     public JSONObject deleteSubscription(String id) throws JSONException {
        // Get the subscription
        FeedSubscriptionDao feedSubscriptionDao = new FeedSubscriptionDao();
        FeedSubscription feedSubscription = feedSubscriptionDao.getFeedSubscription(id, authenticationService.getPrincipal().getId());
        if (feedSubscription == null) {
            throw new ClientException("SubscriptionNotFound", MessageFormat.format("Subscription not found: {0}", id));
        }

        // Delete the subscription
        feedSubscriptionDao.delete(id);

        // Always return ok
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return response;
    }


    public JSONObject syncSubscription(String id) throws JSONException {
        // Get the subscription
        FeedSubscriptionCriteria feedSubscriptionCriteria = new FeedSubscriptionCriteria()
                .setId(id)
                .setUserId(authenticationService.getPrincipal().getId());

        FeedSubscriptionDao feedSubscriptionDao = new FeedSubscriptionDao();
        List<FeedSubscriptionDto> feedSubscriptionList = feedSubscriptionDao.findByCriteria(feedSubscriptionCriteria);
        if (feedSubscriptionList.isEmpty()) {
            throw new ClientException("SubscriptionNotFound", MessageFormat.format("Subscription not found: {0}", id));
        }
        FeedSubscriptionDto feedSubscription = feedSubscriptionList.iterator().next();

        // Get the feed synchronization
        FeedSynchronizationDao feedSynchronizationDao = new FeedSynchronizationDao();
        List<FeedSynchronization> feedSynchronizationList = feedSynchronizationDao.findByFeedId(feedSubscription.getFeedId());

        // Build the response
        JSONObject response = new JSONObject();

        List<JSONObject> synchronizationsJson = new ArrayList<JSONObject>();
        for (FeedSynchronization feedSynchronization : feedSynchronizationList) {
            JSONObject synchronizationJson = new JSONObject();
            synchronizationJson.put("success", feedSynchronization.isSuccess());
            synchronizationJson.put("message", feedSynchronization.getMessage());
            synchronizationJson.put("duration", feedSynchronization.getDuration());
            synchronizationJson.put("create_date", feedSynchronization.getCreateDate().getTime());
            synchronizationsJson.add(synchronizationJson);
        }
        response.put("synchronizations", synchronizationsJson);
        return response;
    }


    public JSONObject markAllRead(String id) throws JSONException {
        // Get the subscription
        FeedSubscriptionDao feedSubscriptionDao = new FeedSubscriptionDao();
        FeedSubscription feedSubscription = feedSubscriptionDao.getFeedSubscription(id, authenticationService.getPrincipal().getId());
        if (feedSubscription == null) {
            throw new ClientException("SubscriptionNotFound", MessageFormat.format("Subscription not found: {0}", id));
        }

        // Marks all articles as read in this subscription
        UserArticleDao userArticleDao = new UserArticleDao();
        userArticleDao.markAsRead(new UserArticleCriteria()
                .setUserId(authenticationService.getPrincipal().getId())
                .setSubscribed(true)
                .setFeedSubscriptionId(id));

        feedSubscriptionDao.updateUnreadCount(feedSubscription.getId(), 0);

        // Always return ok
        return new JSONObject();
    }

}

