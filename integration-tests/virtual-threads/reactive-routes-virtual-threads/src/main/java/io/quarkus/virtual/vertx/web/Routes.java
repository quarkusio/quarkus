package io.quarkus.virtual.vertx.web;

import io.quarkus.vertx.web.Route;
import io.smallrye.common.annotation.RunOnVirtualThread;

public class Routes {

    @RunOnVirtualThread
    @Route
    String hello() {
        AssertHelper.assertEverything();
        // Quarkus specific - each VT has a unique name
        return Thread.currentThread().getName();
    }

}
