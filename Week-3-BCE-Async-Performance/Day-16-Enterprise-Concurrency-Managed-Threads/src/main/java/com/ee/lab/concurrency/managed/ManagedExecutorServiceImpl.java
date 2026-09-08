package com.ee.lab.concurrency.managed;

import com.ee.lab.concurrency.context.CapturedContext;
import com.ee.lab.concurrency.context.SecurityContextHolder;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedExecutorService;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Enterprise ManagedExecutorService implementation for Jakarta Concurrency 3.0.
 * Wraps worker task submissions with automatic Context Propagation:
 * Captures caller thread security/correlation context and restores it on the managed worker thread.
 */
public class ManagedExecutorServiceImpl implements ManagedExecutorService {

    private final ExecutorService delegate;
    private final ScheduledExecutorService scheduler;
    private final ManagedThreadFactoryImpl threadFactory;

    public ManagedExecutorServiceImpl(int corePoolSize, String poolPrefix) {
        this.threadFactory = new ManagedThreadFactoryImpl(poolPrefix);
        this.delegate = new ThreadPoolExecutor(
                corePoolSize,
                corePoolSize * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = threadFactory.newThread(runnable);
            t.setName(poolPrefix + "-timeout-scheduler");
            return t;
        });
    }

    public <T> CompletableFuture<T> applyTimeout(CompletableFuture<T> future, T fallbackValue, long timeout, TimeUnit unit) {
        ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> future.complete(fallbackValue), timeout, unit);
        future.whenComplete((res, ex) -> timeoutTask.cancel(false));
        return future;
    }

    /**
     * Executes asynchronous task using Java CompletableFuture while propagating container context.
     */
    @Override
    public <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        CapturedContext captured = SecurityContextHolder.capture();
        return CompletableFuture.supplyAsync(() -> {
            CapturedContext original = SecurityContextHolder.get();
            try {
                SecurityContextHolder.restore(captured);
                return supplier.get();
            } finally {
                SecurityContextHolder.restore(original);
            }
        }, delegate);
    }

    /**
     * Executes asynchronous runnable while propagating container context.
     */
    @Override
    public CompletableFuture<Void> runAsync(Runnable runnable) {
        CapturedContext captured = SecurityContextHolder.capture();
        return CompletableFuture.runAsync(() -> {
            CapturedContext original = SecurityContextHolder.get();
            try {
                SecurityContextHolder.restore(captured);
                runnable.run();
            } finally {
                SecurityContextHolder.restore(original);
            }
        }, delegate);
    }

    @Override
    public <U> CompletableFuture<U> completedFuture(U value) {
        return CompletableFuture.completedFuture(value);
    }

    @Override
    public <U> CompletionStage<U> completedStage(U value) {
        return CompletableFuture.completedStage(value);
    }

    @Override
    public <T> CompletableFuture<T> copy(CompletableFuture<T> stage) {
        return stage.copy();
    }

    @Override
    public <T> CompletionStage<T> copy(CompletionStage<T> stage) {
        return stage.toCompletableFuture().copy();
    }

    @Override
    public <U> CompletableFuture<U> failedFuture(Throwable ex) {
        return CompletableFuture.failedFuture(ex);
    }

    @Override
    public <U> CompletionStage<U> failedStage(Throwable ex) {
        return CompletableFuture.failedStage(ex);
    }

    @Override
    public ContextService getContextService() {
        return null;
    }

    @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
        return new CompletableFuture<>();
    }

    private Runnable wrap(Runnable task) {
        CapturedContext captured = SecurityContextHolder.capture();
        return () -> {
            CapturedContext original = SecurityContextHolder.get();
            try {
                SecurityContextHolder.restore(captured);
                task.run();
            } finally {
                SecurityContextHolder.restore(original);
            }
        };
    }

    private <T> Callable<T> wrap(Callable<T> task) {
        CapturedContext captured = SecurityContextHolder.capture();
        return () -> {
            CapturedContext original = SecurityContextHolder.get();
            try {
                SecurityContextHolder.restore(captured);
                return task.call();
            } finally {
                SecurityContextHolder.restore(original);
            }
        };
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(wrap(command));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(wrap(task), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public void shutdown() {
        scheduler.shutdownNow();
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        scheduler.shutdownNow();
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasks.stream().map(this::wrap).toList());
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(tasks.stream().map(this::wrap).toList(), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks.stream().map(this::wrap).toList());
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks.stream().map(this::wrap).toList(), timeout, unit);
    }
}
