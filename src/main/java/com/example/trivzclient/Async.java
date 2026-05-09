package com.example.trivzclient;

import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.function.Consumer;

/**
 * Tiny helper that all screens use to fire a blocking call (HTTP, JPA-backed)
 * off the FX thread and route the result/error back onto it. Replaces a runAsync
 * method that was duplicated in every screen.
 */
public final class Async {

    private Async() {}

    @FunctionalInterface
    public interface SafeCallable<T> {
        T call() throws Exception;
    }

    /**
     * Runs `work` on a daemon thread, then on success calls `onOk` on the FX thread,
     * and on failure calls `onErr` on the FX thread with the exception's message.
     *
     * `threadName` is used to name the worker thread (helps debugging).
     */
    public static <T> void run(String threadName,
                               SafeCallable<T> work,
                               Consumer<T> onOk,
                               Consumer<String> onErr) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> onOk.accept(task.getValue())));
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            String msg = (t == null) ? "error" : t.getMessage();
            Platform.runLater(() -> onErr.accept(msg));
        });
        Thread th = new Thread(task, threadName);
        th.setDaemon(true);
        th.start();
    }

    /**
     * Fire-and-forget: runs `work` on a daemon thread, ignores success, routes
     * any error to `onErr` on the FX thread.
     */
    public static void run(String threadName,
                           SafeCallable<?> work,
                           Consumer<String> onErr) {
        Task<Object> task = new Task<>() {
            @Override protected Object call() throws Exception { return work.call(); }
        };
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            String msg = (t == null) ? "error" : t.getMessage();
            Platform.runLater(() -> onErr.accept(msg));
        });
        Thread th = new Thread(task, threadName);
        th.setDaemon(true);
        th.start();
    }
}
