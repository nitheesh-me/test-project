package com.sismics.reader.web.listener;

import com.sismics.reader.core.constant.ConfigType;
import com.sismics.reader.core.dao.jpa.ConfigDao;
import com.sismics.reader.core.model.context.AppContext;
import com.sismics.reader.core.model.jpa.Config;
import com.sismics.reader.core.service.FeedService;
import com.sismics.reader.core.service.IndexingService;
import com.sismics.reader.core.util.AsyncTaskManager;
import com.sismics.reader.core.event.EventBusManager;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.concurrent.Executors;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

/**
 * Initializes application dependencies when the web app starts.
 */
public class StartupListener implements ServletContextListener {
    private FeedService feedService;
    private IndexingService indexingService;

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        EventBusManager eventBusManager = new EventBusManager();
        AsyncTaskManager asyncTaskManager = new AsyncTaskManager();
        // EventBus asyncEventBus = new AsyncEventBus(Executors.newCachedThreadPool());

        // Initialize services with dependency injection
        feedService = new FeedService();

        ConfigDao configDao = new ConfigDao();
        Config luceneStorageConfig = configDao.getById(ConfigType.LUCENE_DIRECTORY_STORAGE);
        indexingService = new IndexingService(luceneStorageConfig != null ? luceneStorageConfig.getValue() : null);

        // Initialize application context with dependencies
        AppContext.initialize(feedService, indexingService, eventBusManager, asyncTaskManager);

        // Start background tasks
        feedService.start();
        indexingService.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Stop background services when shutting down
        feedService.stop();
        indexingService.stop();
    }
}
