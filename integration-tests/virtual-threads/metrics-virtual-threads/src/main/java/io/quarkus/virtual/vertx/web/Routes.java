package io.quarkus.virtual.vertx.web;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkus.micrometer.runtime.binder.virtualthreads.VirtualThreadCollector;
import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.quarkus.vertx.web.Route;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;

public class Routes {

    @Inject
    Instance<VirtualThreadCollector> collector;

    void assertThatTheBinderIsAvailable() {
        if (!collector.isResolvable()) {
            throw new AssertionError("VirtualThreadCollector expected");
        }
    }

    @RunOnVirtualThread
    @Route
    String hello() {
        assertThatTheBinderIsAvailable();
        VirtualThreadsAssertions.assertEverything();
        // Quarkus specific - each VT has a unique name
        return Thread.currentThread().getName();
    }

    @Route
    String ping() {
        assertThatTheBinderIsAvailable();
        VirtualThreadsAssertions.assertWorkerOrEventLoopThread();
        return "pong";
    }

    @Blocking
    @Route
    String blockingPing() {
        assertThatTheBinderIsAvailable();
        return ping();
    }

}
