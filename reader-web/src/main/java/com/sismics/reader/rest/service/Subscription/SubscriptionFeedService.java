package com.sismics.reader.rest.service.Subscription;

import com.sismics.reader.core.dao.jpa.CategoryDao;
import com.sismics.reader.core.dao.jpa.FeedSubscriptionDao;
import com.sismics.reader.core.dao.jpa.UserArticleDao;
import com.sismics.reader.core.dao.jpa.criteria.FeedSubscriptionCriteria;
import com.sismics.reader.core.dao.jpa.criteria.UserArticleCriteria;
import com.sismics.reader.core.dao.jpa.dto.FeedSubscriptionDto;
import com.sismics.reader.core.dao.jpa.dto.UserArticleDto;
import com.sismics.reader.core.model.jpa.Category;
import com.sismics.reader.core.util.jpa.PaginatedList;
import com.sismics.reader.core.util.jpa.PaginatedLists;
import com.sismics.reader.rest.assembler.ArticleAssembler;
import com.sismics.reader.rest.service.Authentication.AuthencticationService;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.util.JsonUtil;
import com.sismics.security.IPrincipal;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SubscriptionFeedService {
    private final IPrincipal principal;
    private final UserArticleDao userArticleDao;
    private final UserArticleCriteria userArticleCriteria;
    private final CategoryDao categoryDao;
    private final UserArticleCriteria afterArticleCriteria;
    private final FeedSubscriptionDao feedSubscriptionDao;
    private final FeedSubscriptionCriteria feedSubscriptionCriteria;

    public SubscriptionFeedService(@Context HttpServletRequest request){
        this.userArticleDao = new UserArticleDao();
        AuthencticationService authService = new AuthencticationService(request);
        this.userArticleCriteria = new UserArticleCriteria();
        this.categoryDao = new CategoryDao();
        this.afterArticleCriteria = new UserArticleCriteria();
        this.principal = authService.getPrincipal();
        this.feedSubscriptionDao = new FeedSubscriptionDao();
        this.feedSubscriptionCriteria = new FeedSubscriptionCriteria();

    }

    public JSONObject getSubsNCategories(boolean unread) throws JSONException {
        // Search this user's subscriptions
        List<FeedSubscriptionDto> feedSubscriptionList = getFeedSubscriptionDtos(unread);

        // Get the root category

        Category rootCategory = categoryDao.getRootCategory(principal.getId());
        JSONObject rootCategoryJson = new JSONObject();
        rootCategoryJson.put("id", rootCategory.getId());

        // Construct the response
        List<JSONObject> rootCategories = new ArrayList<JSONObject>();
        rootCategories.add(rootCategoryJson);
        String oldCategoryId = null;
        JSONObject categoryJson = rootCategoryJson;
        int totalUnreadCount = 0;
        int categoryUnreadCount = 0;
        for (FeedSubscriptionDto feedSubscription : feedSubscriptionList) {
            String categoryId = feedSubscription.getCategoryId();
            String categoryParentId = feedSubscription.getCategoryParentId();

            if (!categoryId.equals(oldCategoryId)) {
                if (categoryParentId != null) {
                    if (categoryJson != rootCategoryJson) {
                        categoryJson.put("unread_count", categoryUnreadCount);
                        JsonUtil.append(rootCategoryJson, "categories", categoryJson);
                    }
                    categoryJson = new JSONObject();
                    categoryJson.put("id", categoryId);
                    categoryJson.put("name", feedSubscription.getCategoryName());
                    categoryJson.put("folded", feedSubscription.isCategoryFolded());
                    categoryJson.put("subscriptions", new JSONArray());
                    categoryUnreadCount = 0;
                }
            }
            JSONObject subscription = new JSONObject();
            subscription.put("id", feedSubscription.getId());
            subscription.put("title", feedSubscription.getFeedSubscriptionTitle());
            subscription.put("url", feedSubscription.getFeedRssUrl());
            subscription.put("unread_count", feedSubscription.getUnreadUserArticleCount());
            subscription.put("sync_fail_count", feedSubscription.getSynchronizationFailCount());
            JsonUtil.append(categoryJson, "subscriptions", subscription);

            oldCategoryId = categoryId;
            categoryUnreadCount += feedSubscription.getUnreadUserArticleCount();
            totalUnreadCount += feedSubscription.getUnreadUserArticleCount();
        }
        if (categoryJson != rootCategoryJson) {
            categoryJson.put("unread_count", categoryUnreadCount);
            JsonUtil.append(rootCategoryJson, "categories", categoryJson);
        }

        // Add the categories without subscriptions
        if (!unread) {
            List<Category> allCategoryList = categoryDao.findSubCategory(rootCategory.getId(), principal.getId());
            JSONArray categoryArrayJson = rootCategoryJson.optJSONArray("categories");
            List<JSONObject> fullCategoryListJson = new ArrayList<JSONObject>();
            int i = 0;
            for (Category category : allCategoryList) {
                if (categoryArrayJson != null && i < categoryArrayJson.length() && categoryArrayJson.getJSONObject(i).getString("id").equals(category.getId())) {
                    categoryJson = categoryArrayJson.getJSONObject(i++);
                } else {
                    categoryJson = new JSONObject();
                    categoryJson.put("id", category.getId());
                    categoryJson.put("name", category.getName());
                    categoryJson.put("folded", category.isFolded());
                    categoryJson.put("unread_count", 0);
                }
                fullCategoryListJson.add(categoryJson);
            }
            rootCategoryJson.put("categories", fullCategoryListJson);
        }

        JSONObject response = new JSONObject();
        response.put("categories", rootCategories);
        response.put("unread_count", totalUnreadCount);
        return response;
    }

    private List<FeedSubscriptionDto> getFeedSubscriptionDtos(boolean unread) {
        feedSubscriptionCriteria
                .setUserId(principal.getId())
                .setUnread(unread);

        List<FeedSubscriptionDto> feedSubscriptionList = feedSubscriptionDao.findByCriteria(feedSubscriptionCriteria);
        return feedSubscriptionList;
    }

    public JSONObject getSubsNPage(String id, boolean unread, Integer limit, String afterArticle) throws JSONException {
        FeedSubscriptionDto feedSubscription = getFeedSubscriptionDto(id);

        // Get the articles
        UserArticleCriteria userArticleCriteria = getArticleCriteria(unread, afterArticle, feedSubscription);

        PaginatedList<UserArticleDto> paginatedList = PaginatedLists.create(limit, null);
        userArticleDao.findByCriteria(paginatedList, userArticleCriteria, null, null);

        // Build the response
        return getResponse(feedSubscription, paginatedList);
    }

    private UserArticleCriteria getArticleCriteria(boolean unread, String afterArticle, FeedSubscriptionDto feedSubscription) throws JSONException {
        userArticleCriteria.
                setUnread(unread)
                .setUserId(principal.getId())
                .setSubscribed(true)
                .setVisible(true)
                .setFeedId(feedSubscription.getFeedId());

        if (afterArticle != null) {
            // Paginate after this user article
            afterArticleCriteria.setUserArticleId(afterArticle)
                    .setUserId(principal.getId());
            List<UserArticleDto> userArticleDtoList = userArticleDao.findByCriteria(afterArticleCriteria);
            if (userArticleDtoList.isEmpty()) {
                throw new ClientException("ArticleNotFound", MessageFormat.format("Can't find user article {0}", afterArticle));
            }
            UserArticleDto userArticleDto = userArticleDtoList.iterator().next();

            userArticleCriteria.setArticlePublicationDateMax(new Date(userArticleDto.getArticlePublicationTimestamp()));
            userArticleCriteria.setArticleIdMax(userArticleDto.getArticleId());
        }
        return userArticleCriteria;
    }

    private FeedSubscriptionDto getFeedSubscriptionDto(String id) throws JSONException {
        // Get the subscription
        feedSubscriptionCriteria.setId(id).setUserId(principal.getId());

        List<FeedSubscriptionDto> feedSubscriptionList = feedSubscriptionDao.findByCriteria(feedSubscriptionCriteria);
        if (feedSubscriptionList.isEmpty()) {
            throw new ClientException("SubscriptionNotFound", MessageFormat.format("Subscription not found: {0}", id));
        }
        FeedSubscriptionDto feedSubscription = feedSubscriptionList.iterator().next();
        return feedSubscription;
    }

    private static JSONObject getResponse(FeedSubscriptionDto feedSubscription, PaginatedList<UserArticleDto> paginatedList) throws JSONException {
        JSONObject response = new JSONObject();

        JSONObject subscription = new JSONObject();
        subscription.put("title", feedSubscription.getFeedSubscriptionTitle());
        subscription.put("feed_title", feedSubscription.getFeedTitle());
        subscription.put("url", feedSubscription.getFeedUrl());
        subscription.put("rss_url", feedSubscription.getFeedRssUrl());
        subscription.put("description", feedSubscription.getFeedDescription());
        subscription.put("category_id", feedSubscription.getCategoryId());
        subscription.put("category_name", feedSubscription.getCategoryName());
        subscription.put("create_date", feedSubscription.getCreateDate().getTime());
        response.put("subscription", subscription);

        List<JSONObject> articles = new ArrayList<JSONObject>();
        for (UserArticleDto userArticle : paginatedList.getResultList()) {
            articles.add(ArticleAssembler.asJson(userArticle));
        }
        response.put("articles", articles);
        return response;
    }

}
