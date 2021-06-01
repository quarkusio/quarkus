package io.quarkus.grpc.server.services;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.arc.Arc;
import io.vertx.core.Vertx;

public class AssertHelper {

    public static void assertThatTheRequestScopeIsActive() {
        assertThat(Arc.container().requestContext().isActive()).isTrue();
    }

    public static void assertRunOnEventLoop() {
        assertThat(Vertx.currentContext()).isNotNull();
        assertThat(Vertx.currentContext().isEventLoopContext());
        assertThat(Thread.currentThread().getName()).contains("eventloop");
    }

    public static void assertRunOnWorker() {
        assertThat(Vertx.currentContext()).isNotNull();
        assertThat(Thread.currentThread().getName()).contains("executor");
    }

}
