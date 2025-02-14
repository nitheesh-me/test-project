package com.sismics.reader.core.event;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.sismics.reader.core.listener.async.*;
import com.sismics.reader.core.listener.sync.DeadEventListener;

import java.util.concurrent.Executors;

/**
 * Manages Event Buses for different event types.
 */
public class EventBusManager {
    
    /**
     * Event bus.
     */
    private EventBus eventBus;
    
    /**
     * Generic asynchronous event bus.
     */
    private EventBus asyncEventBus;

    /**
     * Asynchronous event bus for emails.
     */
    private EventBus mailEventBus;
    
    /**
     * Asynchronous event bus for mass imports.
     */
    private EventBus importEventBus;

    public EventBusManager() {
        this.eventBus = new EventBus();
        this.asyncEventBus = new AsyncEventBus(Executors.newCachedThreadPool());
        this.mailEventBus = new AsyncEventBus(Executors.newCachedThreadPool());
        this.importEventBus = new AsyncEventBus(Executors.newCachedThreadPool());

        registerListeners();
    }

    private void registerListeners() {
        eventBus.register(new DeadEventListener());

        asyncEventBus.register(new ArticleCreatedAsyncListener());
        asyncEventBus.register(new ArticleUpdatedAsyncListener());
        asyncEventBus.register(new ArticleDeletedAsyncListener());
        asyncEventBus.register(new RebuildIndexAsyncListener());
        asyncEventBus.register(new FaviconUpdateRequestedAsyncListener());

        importEventBus.register(new SubscriptionImportAsyncListener());
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public EventBus getAsyncEventBus() {
        return asyncEventBus;
    }

    public EventBus getMailEventBus() {
        return mailEventBus;
    }

    public EventBus getImportEventBus() {
        return importEventBus;
    }
}
