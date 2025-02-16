package com.sismics.reader.core.model.context;

import com.sismics.reader.core.service.FeedService;
import com.sismics.reader.core.service.IFeedService;
import com.sismics.reader.core.service.IIndexingService;
import com.sismics.reader.core.service.IndexingService;
import com.sismics.reader.core.util.AsyncTaskManager;
import com.sismics.reader.core.event.EventBusManager;

/**
 * Global application context.
 *
 * @author jtremeaux 
 */
public class AppContext {
    private static AppContext instance;
    private final IFeedService feedService;
    private final IIndexingService indexingService;
    private final EventBusManager eventBusManager;
    private final AsyncTaskManager asyncTaskManager;

    private AppContext(Builder builder) {
        this.feedService = builder.feedService;
        this.indexingService = builder.indexingService;
        this.eventBusManager = builder.eventBusManager;
        this.asyncTaskManager = builder.asyncTaskManager;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static void initialize(FeedService feedService, IndexingService indexingService, EventBusManager eventBusManager, AsyncTaskManager asyncTaskManager) {
        if (instance == null) {
            instance = builder()
                .feedService(feedService)
                .indexingService(indexingService)
                .eventBusManager(eventBusManager)
                .asyncTaskManager(asyncTaskManager)
                .build();
        }
    }

    public static class Builder {
        private IFeedService feedService;
        private IIndexingService indexingService;
        private EventBusManager eventBusManager;
        private AsyncTaskManager asyncTaskManager;

        public Builder feedService(FeedService feedService) {
            this.feedService = feedService;
            return this;
        }

        public Builder indexingService(IIndexingService indexingService) {
            this.indexingService = indexingService;
            return this;
        }

        public Builder eventBusManager(EventBusManager eventBusManager) {
            this.eventBusManager = eventBusManager;
            return this;
        }

        public Builder asyncTaskManager(AsyncTaskManager asyncTaskManager) {
            this.asyncTaskManager = asyncTaskManager;
            return this;
        }

        public AppContext build() {
            return new AppContext(this);
        }
    }

    public static void initialize(AppContext context) {
        if (instance == null) {
            instance = context;
        }
    }

    public static AppContext getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppContext has not been initialized");
        }
        return instance;
    }

    public EventBusManager getEventBusManager() {
        return eventBusManager;
    }

    public IFeedService getFeedService() {
        return feedService;
    }

    public IIndexingService getIndexingService() {
        return indexingService;
    }

    public void waitForAsyncCompletion() {
        asyncTaskManager.waitForAsyncCompletion();
    }
}
