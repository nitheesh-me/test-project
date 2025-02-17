package com.sismics.reader.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Manages asynchronous task execution.
 */
public class AsyncTaskManager {
    /**
     * Asynchronous executors.
     */
    private List<ExecutorService> asyncExecutorList;
    
    public AsyncTaskManager() {
        this.asyncExecutorList = new ArrayList<ExecutorService>();
    }

    public ExecutorService createExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        asyncExecutorList.add(executor);
        return executor;
    }

    public void waitForAsyncCompletion() {
        try {
            for (ExecutorService executor : asyncExecutorList) {
                executor.shutdown();
                executor.awaitTermination(60, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
