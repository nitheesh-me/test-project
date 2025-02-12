package com.sismics.reader.core.model.context;

import com.sismics.reader.core.service.FeedService;
import com.sismics.reader.core.service.IndexingService;
import com.sismics.reader.core.util.AsyncTaskManager;
import com.sismics.reader.core.event.EventBusManager;

/**
 * Global application context.
 *
 * @author jtremeaux 
 */
public class AppContext {
    /**
     * Singleton instance.
     */
    private static AppContext instance;

    /**
     * Feed service.
     */
    private FeedService feedService;
    
    /**
     * Indexing service.
     */
    private IndexingService indexingService;

    /**
     * Event Bus Manager
     */
    private EventBusManager eventBusManager;

    /**
     * Async Task Manager
     */
    private AsyncTaskManager asyncTaskManager;

    /**
     * Private constructor.
     */
    private AppContext(FeedService feedService, IndexingService indexingService, EventBusManager eventBusManager, AsyncTaskManager asyncTaskManager) {
        this.feedService = feedService;
        this.indexingService = indexingService;
        this.eventBusManager = eventBusManager;
        this.asyncTaskManager = asyncTaskManager;
    }
    
    /**
     * Initializes the application context with the given dependencies.
     * 
     * @param feedService Feed service
     * @param indexingService Indexing service
     */
    public static void initialize(FeedService feedService, IndexingService indexingService, EventBusManager eventBusManager, AsyncTaskManager asyncTaskManager) {
        if (instance == null) {
            instance = new AppContext(feedService, indexingService, eventBusManager, asyncTaskManager);
        }
    }

    /**
     * Returns a single instance of the application context.
     * 
     * @return Application context
     */
    public static AppContext getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppContext has not been initialized");
        }
        return instance;
    }
    
    /**
     * Getter of eventBusManager.
     *
     * @return eventBusManager
     */
    public EventBusManager getEventBusManager() {
        return eventBusManager;
    }

    /**
     * Getter of feedService.
     *
     * @return feedService
     */
    public FeedService getFeedService() {
        return feedService;
    }
    
    /**
     * Getter of indexingService.
     *
     * @return indexingService
     */
    public IndexingService getIndexingService() {
        return indexingService;
    }

    /**
     * wait for AsyncCompletion
     */
    public void waitForAsyncCompletion() {
        asyncTaskManager.waitForAsyncCompletion();
    }
}
