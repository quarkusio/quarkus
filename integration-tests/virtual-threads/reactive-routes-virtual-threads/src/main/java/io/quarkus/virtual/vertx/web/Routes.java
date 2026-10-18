package io.quarkus.virtual.vertx.web;

import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.quarkus.vertx.web.Route;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;

public class Routes {

    @RunOnVirtualThread
    @Route
    String hello() {
        VirtualThreadsAssertions.assertEverything();
        // Quarkus specific - each VT has a unique name
        return Thread.currentThread().getName();
    }

    @Route
    String ping() {
        VirtualThreadsAssertions.assertWorkerOrEventLoopThread();
        return "pong";
    }

    @Blocking
    @Route
    String blockingPing() {
        return ping();
    }

}
