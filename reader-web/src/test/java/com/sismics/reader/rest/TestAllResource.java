package com.sismics.reader.rest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import junit.framework.Assert;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertTrue;

/**
 * Exhaustive test of the all resource.
 *
 * @author jtremeaux
 */
@Ignore
public class TestAllResource extends BaseJerseyTest {
    /**
     * Test of the all resource.
     *
     */
    @Test
    public void testAllResource() throws JSONException {
        // Create user all1
        createUser("all1");
        login("all1");

        // Subscribe to korben.info
        PUT("/subscription", ImmutableMap.of("url", "http://localhost:9997/http/feeds/korben.xml"));
        assertIsOk();
        JSONObject json = getJsonResult();
        String subscription0Id = json.optString("id");
        assertNotNull(subscription0Id);

        // Check the category tree
        GET("/category");
        assertIsOk();
        json = getJsonResult();
        JSONArray categories = json.optJSONArray("categories");
        assertNotNull(categories);
        assertEquals(1, categories.length());
        JSONObject rootCategory = categories.optJSONObject(0);
        String rootCategoryId = rootCategory.optString("id");
        assertNotNull(rootCategoryId);
        categories = rootCategory.optJSONArray("categories");
        assertEquals(0, categories.length());

        // Check the root category
        GET("/category/" + rootCategoryId);
        assertIsOk();
        json = getJsonResult();
        JSONArray articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(10, articles.length());
        JSONObject article = (JSONObject) articles.get(1);
        String article1Id = article.getString("id");
        article = (JSONObject) articles.get(2);
        String article2Id = article.getString("id");

        // Check pagination
        GET("/category/" + rootCategoryId, ImmutableMap.of("after_article", article1Id));
        assertIsOk();
        json = getJsonResult();
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(8, articles.length());
        assertEquals(article2Id, article.getString("id"));

        // Check the all resource
        GET("/all");
        assertIsOk();
        json = getJsonResult();
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(10, articles.length());
        article = (JSONObject) articles.get(1);
        article1Id = article.getString("id");
        article = (JSONObject) articles.get(2);
        article2Id = article.getString("id");

        // Check pagination
        GET("/all", ImmutableMap.of("after_article", article1Id));
        assertIsOk();
        json = getJsonResult();
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(8, articles.length());
        assertEquals(article2Id, article.getString("id"));

        // Marks all articles as read
        POST("/all/read");
        assertIsOk();

        // Check the all resource
        GET("/all");
        assertIsOk();
        json = getJsonResult();
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(10, articles.length());

        // Check in the subscriptions that there are no unread articles left
        GET("/subscription", ImmutableMap.of("after_article", article1Id));
        assertIsOk();
        json = getJsonResult();
        assertEquals(0, json.optInt("unread_count"));
        categories = json.getJSONArray("categories");
        rootCategory = categories.getJSONObject(0);
        JSONArray subscriptions = rootCategory.getJSONArray("subscriptions");
        JSONObject subscription0 = subscriptions.getJSONObject(0);
        assertEquals(0, subscription0.optInt("unread_count"));

        // Check the all resource for unread articles
        GET("/all", ImmutableMap.of("unread", Boolean.TRUE.toString()));
        assertIsOk();
        json = getJsonResult();
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(0, articles.length());
    }

    @Test
    public void testMultipleUsers() throws JSONException {
        // Create user multiple1
        createUser("multiple1");
        login("multiple1");

        // Subscribe to korben.info
        PUT("/subscription", ImmutableMap.of("url", "http://localhost:9997/http/feeds/korben.xml"));
        assertIsOk();
        JSONObject json = getJsonResult();
        String subscription0Id = json.optString("id");
        assertNotNull(subscription0Id);

        // Check the all resource
        GET("/all", ImmutableMap.of("unread", "true"));
        assertIsOk();
        json = getJsonResult();
        JSONArray articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(10, articles.length());

        // Create user multiple2
        createUser("multiple2");
        login("multiple2");

        // Subscribe to korben.info (alternative URL)
        PUT("/subscription", ImmutableMap.of("url", "http://localhost:9997/http/feeds/korben2.xml"));
        assertIsOk();
        json = getJsonResult();
        subscription0Id = json.optString("id");
        assertNotNull(subscription0Id);

        // Check the all resource
        GET("/all", ImmutableMap.of("unread", "true"));
        assertIsOk();
        json = getJsonResult();
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(10, articles.length());
    }

    /**
     * Test of the search resource.
     */
    @Test
    public void testSearchResource() throws Exception {
        // Create user search1
        createUser("search1");
        login("search1");

        // Subscribe to Korben RSS feed
        PUT("/subscription", ImmutableMap.of("url", "http://localhost:9997/http/feeds/korben.xml"));
        assertIsOk();

        // Search "zelda": OK, one result
        GET("/search/searchtermzelda");
        assertIsOk();
        JSONObject json = getJsonResult();
        JSONArray articles = json.getJSONArray("articles");
        assertEquals(1, articles.length());
        assertSearchResult(articles, "Quand <span class=\"highlight\">searchtermZelda</span> prend les armes", 0);

        // Search "njloinzejrmklsjd": OK, no result
        GET("/search/njloinzejrmklsjd");
        assertIsOk();
        json = getJsonResult();
        articles = json.getJSONArray("articles");
        assertEquals(0, articles.length());

        // Search "wifi": OK, 2 results
        GET("/search/searchtermwifi");
        assertIsOk();
        json = getJsonResult();
        articles = json.getJSONArray("articles");
        assertEquals(2, articles.length());
        assertSearchResult(articles, "Récupérer les clés <span class=\"highlight\">searchtermwifi</span> sur un téléphone Android", 0);
        assertSearchResult(articles, "Partagez vos clés <span class=\"highlight\">searchtermWiFi</span> avec vos amis", 1);

        // Search "google keep": OK, 2 results
        GET("/search/searchtermgoogle%20searchtermkeep");
        assertIsOk();
        json = getJsonResult();
        articles = json.getJSONArray("articles");
        assertEquals(2, articles.length());
        assertSearchResult(articles, "<span class=\"highlight\">searchtermGoogle</span> <span class=\"highlight\">searchtermKeep</span>…eut pas vraiment en faire plus (pour le moment)", 0);
        assertSearchResult(articles, "Quand searchtermZelda prend les armes", 1);

        // Create user search2
        createUser("search2");
        login("search2");

        // Subscribe to Korben RSS feed again to force articles updating
        PUT("/subscription", ImmutableMap.of("url", "http://localhost:9997/http/feeds/korben.xml"));
        assertIsOk();

        // Check if nothing is broken by searching "google keep"
        GET("/search/searchtermgoogle%20searchtermkeep");
        assertIsOk();
        json = getJsonResult();
        articles = json.getJSONArray("articles");
        assertEquals(2, articles.length());

        // Create user search3
        createUser("search3");
        login("search3");

        // Search "njloinzejrmklsjd"
        GET("/search/njloinzejrmklsjd");
        assertIsOk();
        json = getJsonResult();
        articles = json.getJSONArray("articles");
        assertEquals(0, articles.length());

        // Search "zelda"
        GET("/search/searchtermzelda");
        assertIsOk();
        json = getJsonResult();
        articles = json.getJSONArray("articles");
        assertEquals(1, articles.length());
        assertSearchResult(articles, "Quand <span class=\"highlight\">searchtermZelda</span> prend les armes", 0);

        // Subscribe to Korben RSS feed (alternative URL)
        PUT("/subscription", ImmutableMap.of("url", "http://localhost:9997/http/feeds/korben2.xml"));
        assertIsOk();

        // Search "zelda"
        GET("/search/searchtermzelda");
        assertIsOk();
        json = getJsonResult();
        articles = json.getJSONArray("articles");
        assertEquals(1, articles.length());
        assertSearchResult(articles, "Quand <span class=\"highlight\">searchtermZelda</span> prend les armes", 0);
    }

    /**
     * Assert that an article exists with a specific title in the provided articles set.
     *
     * @param articles Articles from search
     * @param title Expected title
     * @param index Index
     */
    private void assertSearchResult(JSONArray articles, String title, int index) throws JSONException {
        JSONObject article = articles.getJSONObject(index);
        if (article.getString("title").equals(title)) {
            return;
        }
        Assert.fail("[" + title + "] not found in [" + article.getString("title") + "]");
    }
    /**
     * Test of the all resource.
     *
     */
    @Test
    public void testStarredResource() throws JSONException {
        // Create user starred1
        createUser("starred1");
        login("starred1");

        // Subscribe to korben.info
        PUT("/subscription", ImmutableMap.of("url", "http://localhost:9997/http/feeds/korben.xml"));
        assertIsOk();
        JSONObject json = getJsonResult();
        String subscription1Id = json.optString("id");
        assertNotNull(subscription1Id);

        // Check the all resource
        GET("/all");
        assertIsOk();
        json = getJsonResult();
        JSONArray articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(10, articles.length());
        JSONObject article0 = articles.getJSONObject(0);
        String article0Id = article0.getString("id");
        JSONObject article1 = articles.getJSONObject(1);
        String article1Id = article1.getString("id");

        // Create a new starred article
        PUT("/starred/" + article0Id);
        assertIsOk();

        // Create a new starred article
        PUT("/starred/" + article1Id);
        assertIsOk();

        // Check the starred resource
        GET("/starred");
        assertIsOk();
        json = getJsonResult();
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(2, articles.length());
        JSONObject articleAfter = articles.getJSONObject(0);
        String articleAfterId = articleAfter.getString("id");

        // Check pagination
        GET("/starred", ImmutableMap.of("after_article", articleAfterId));
        assertIsOk();
        json = getJsonResult();
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(1, articles.length());

        // Delete a starred article
        DELETE("/starred/" + article0Id);
        assertIsOk();

        // Check the starred resource
        GET("/starred");
        assertIsOk();
        json = getJsonResult();
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(1, articles.length());

        // Delete multiple starred articles
        POST("/starred/unstar", ImmutableMultimap.of(
                "id", article0Id,
                "id", article1Id));
        assertIsOk();

        // Check the starred resource
        GET("/starred");
        assertIsOk();
        json = getJsonResult();
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(0, articles.length());

        // Create multiple starred articles
        POST("/starred/star", ImmutableMultimap.of(
                "id", article0Id,
                "id", article1Id));
        assertIsOk();

        // Check the starred resource
        GET("/starred");
        assertIsOk();
        json = getJsonResult();
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(2, articles.length());
    }

    /**
     * Test of the category resource.
     *
     */
    @Test
    public void testCategoryResource() throws JSONException {
        // Create user category1
        createUser("category1");
        login("category1");

        // Create a category : KO (name required)
        PUT("/category", ImmutableMap.of("name", " "));
        assertIsBadRequest();
        JSONObject json = getJsonResult();
        assertEquals("ValidationError", json.getString("type"));
        assertTrue(json.getString("message"), json.getString("message").contains("more than 1"));

        // Create a category
        PUT("/category", ImmutableMap.of("name", "techno"));
        assertIsOk();
        json = getJsonResult();
        String category1Id = json.optString("id");
        assertNotNull(category1Id);

        // Create a category
        PUT("/category", ImmutableMap.of("name", "comics"));
        assertIsOk();
        json = getJsonResult();
        String category2Id = json.optString("id");
        assertNotNull(category2Id);

        // Subscribe to korben.info
        PUT("/subscription", ImmutableMap.of("url", "http://localhost:9997/http/feeds/korben.xml"));
        assertIsOk();
        json = getJsonResult();
        String subscription1Id = json.optString("id");
        assertNotNull(subscription1Id);

        // Check the category tree
        GET("/category");
        assertIsOk();
        json = getJsonResult();
        JSONArray categories = json.optJSONArray("categories");
        assertNotNull(categories);
        assertEquals(1, categories.length());
        JSONObject rootCategory = categories.optJSONObject(0);
        String rootCategoryId = rootCategory.optString("id");
        assertNotNull(rootCategoryId);
        categories = rootCategory.optJSONArray("categories");
        assertEquals(2, categories.length());
        JSONObject technologyCategory = categories.optJSONObject(0);
        assertEquals(category1Id, technologyCategory.optString("id"));
        assertEquals("techno", technologyCategory.optString("name"));
        JSONObject comicsCategory = categories.optJSONObject(1);
        assertEquals(category2Id, comicsCategory.optString("id"));
        assertEquals("comics", comicsCategory.optString("name"));

        // Check the root category
        GET("/category/" + rootCategoryId);
        assertIsOk();
        json = getJsonResult();
        JSONArray articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(10, articles.length());

        // Move the korben.info subscription to "techno"
        POST("/subscription/" + subscription1Id, ImmutableMap.of("category", category1Id));
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));

        // Subscribe to xkcd.com
        PUT("/subscription", ImmutableMap.of("url", "http://localhost:9997/http/feeds/xkcd.xml"));
        assertIsOk();
        json = getJsonResult();
        String subscription2Id = json.optString("id");
        assertNotNull(subscription2Id);

        // Move the xkcd.com subscription to "comics"
        POST("/subscription/" + subscription2Id, ImmutableMap.of("category", category2Id));
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));

        // List all subscriptions
        GET("/subscription");
        assertIsOk();
        json = getJsonResult();
        categories = json.optJSONArray("categories");
        assertNotNull(categories);
        assertEquals(1, categories.length());
        rootCategory = categories.optJSONObject(0);
        categories = rootCategory.optJSONArray("categories");
        assertEquals(2, categories.length());
        technologyCategory = categories.optJSONObject(0);
        assertEquals("techno", technologyCategory.optString("name"));
        assertEquals(false, technologyCategory.optBoolean("folded"));
        JSONArray subscriptions = technologyCategory.optJSONArray("subscriptions");
        assertEquals(1, subscriptions.length());
        comicsCategory = categories.optJSONObject(1);
        assertEquals("comics", comicsCategory.optString("name"));
        assertEquals(false, comicsCategory.optBoolean("folded"));
        subscriptions = comicsCategory.optJSONArray("subscriptions");
        assertEquals(1, subscriptions.length());

        // Update a category
        POST("/category/" + category1Id, ImmutableMap.of(
                "name", "technology",
                "order", "1",
                "folded", Boolean.TRUE.toString()
        ));
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));

        // Check category changes
        GET("/subscription");
        assertIsOk();
        json = getJsonResult();
        int unreadCount = json.optInt("unread_count");
        assertTrue(unreadCount > 0);
        categories = json.optJSONArray("categories");
        assertNotNull(categories);
        assertEquals(1, categories.length());
        rootCategory = categories.optJSONObject(0);
        categories = rootCategory.optJSONArray("categories");
        assertEquals(2, categories.length());
        comicsCategory = categories.optJSONObject(0);
        assertEquals("comics", comicsCategory.optString("name"));
        assertEquals(false, comicsCategory.optBoolean("folded"));
        Integer comicsUnreadCount = comicsCategory.optInt("unread_count");
        assertTrue(comicsUnreadCount > 0);
        technologyCategory = categories.optJSONObject(1);
        assertEquals("technology", technologyCategory.optString("name"));
        assertEquals(true, technologyCategory.optBoolean("folded"));
        assertTrue(technologyCategory.optInt("unread_count") > 0);

        // Marks all articles in the technology category as read
        POST("/category/" + category1Id + "/read");
        assertIsOk();

        // Check the category for unread articles
        GET("/subscription");
        assertIsOk();
        json = getJsonResult();
        assertEquals(comicsUnreadCount.intValue(), json.optInt("unread_count"));
        categories = json.optJSONArray("categories");
        assertNotNull(categories);
        assertEquals(1, categories.length());
        rootCategory = categories.optJSONObject(0);
        categories = rootCategory.optJSONArray("categories");
        assertEquals(2, categories.length());
        comicsCategory = categories.optJSONObject(0);
        assertEquals("comics", comicsCategory.optString("name"));
        assertEquals(false, comicsCategory.optBoolean("folded"));
        assertEquals(comicsUnreadCount.intValue(), comicsCategory.optInt("unread_count"));
        technologyCategory = categories.optJSONObject(1);
        assertEquals("technology", technologyCategory.optString("name"));
        assertEquals(true, technologyCategory.optBoolean("folded"));
        assertEquals(0, technologyCategory.optInt("unread_count"));

        // Check the category for only unread articles
        GET("/subscription", ImmutableMap.of("unread", Boolean.TRUE.toString()));
        assertIsOk();
        json = getJsonResult();
        assertEquals(comicsUnreadCount.intValue(), json.optInt("unread_count"));
        categories = json.optJSONArray("categories");
        assertNotNull(categories);
        assertEquals(1, categories.length());
        rootCategory = categories.optJSONObject(0);
        categories = rootCategory.optJSONArray("categories");
        assertEquals(1, categories.length());
        comicsCategory = categories.optJSONObject(0);
        assertEquals("comics", comicsCategory.optString("name"));
        assertEquals(false, comicsCategory.optBoolean("folded"));
        assertEquals(comicsUnreadCount.intValue(), comicsCategory.optInt("unread_count"));

        // Deletes a category
        DELETE("/category/" + category1Id);
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));
    }
}