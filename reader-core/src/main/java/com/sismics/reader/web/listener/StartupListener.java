package com.sismics.reader.web.listener;

import com.sismics.reader.core.constant.ConfigType;
import com.sismics.reader.core.dao.jpa.ConfigDao;
import com.sismics.reader.core.model.context.AppContext;
import com.sismics.reader.core.model.jpa.Config;
import com.sismics.reader.core.service.FeedService;
import com.sismics.reader.core.service.IndexingService;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Initializes application dependencies when the web app starts.
 */
public class StartupListener implements ServletContextListener {
    private FeedService feedService;
    private IndexingService indexingService;

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        // Initialize services with dependency injection
        feedService = new FeedService();

        ConfigDao configDao = new ConfigDao();
        Config luceneStorageConfig = configDao.getById(ConfigType.LUCENE_DIRECTORY_STORAGE);
        indexingService = new IndexingService(luceneStorageConfig != null ? luceneStorageConfig.getValue() : null);

        // Initialize application context with dependencies
        AppContext.initialize(feedService, indexingService);

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
