package io.quarkus.smallrye.faulttolerance.test.asynchronous.noncompat;

import static io.smallrye.faulttolerance.core.util.CompletionStages.failedFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
@Retry(maxRetries = 3, delay = 0, jitter = 0)
public class NoncompatHelloService {
    private final List<Thread> helloThreads = new CopyOnWriteArrayList<>();
    private final List<StackTraceElement[]> helloStackTraces = new CopyOnWriteArrayList<>();

    private final AtomicInteger invocationCounter = new AtomicInteger();

    private volatile Thread fallbackThread;

    @Fallback(fallbackMethod = "fallback")
    public CompletionStage<String> hello() {
        invocationCounter.incrementAndGet();
        helloThreads.add(Thread.currentThread());
        helloStackTraces.add(new Throwable().getStackTrace());
        return failedFuture(new RuntimeException());
    }

    public CompletionStage<String> fallback() {
        fallbackThread = Thread.currentThread();
        return completedFuture("hello");
    }

    List<Thread> getHelloThreads() {
        return helloThreads;
    }

    List<StackTraceElement[]> getHelloStackTraces() {
        return helloStackTraces;
    }

    AtomicInteger getInvocationCounter() {
        return invocationCounter;
    }

    Thread getFallbackThread() {
        return fallbackThread;
    }
}
