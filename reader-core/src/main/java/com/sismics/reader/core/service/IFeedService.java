// IFeedService.java
package com.sismics.reader.core.service;

import com.sismics.reader.core.model.jpa.Feed;
import com.sismics.reader.core.model.jpa.FeedSubscription;

public interface IFeedService {
    void synchronizeAllFeeds();
    Feed synchronize(String url) throws Exception;
    void createInitialUserArticle(String userId, FeedSubscription feedSubscription);
}