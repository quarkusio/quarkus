package io.quarkus.test.vertx;

import java.lang.reflect.Method;

import io.quarkus.arc.Arc;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Vertx;

public class VirtualThreadsAssertions {

    /**
     * Asserts that the current method: - runs on a duplicated context - runs on a virtual thread - has the request
     * scope activated
     */
    public static void assertEverything() {
        assertThatTheRequestScopeIsActive();
        assertThatItRunsOnVirtualThread();
        assertThatItRunsOnADuplicatedContext();
    }

    public static void assertWorkerOrEventLoopThread() {
        assertThatTheRequestScopeIsActive();
        assertThatItRunsOnADuplicatedContext();
        assertNotOnVirtualThread();
    }

    public static void assertThatTheRequestScopeIsActive() {
        if (!Arc.container().requestContext().isActive()) {
            throw new AssertionError(("Expected the request scope to be active"));
        }
    }

    public static void assertThatItRunsOnADuplicatedContext() {
        var context = Vertx.currentContext();
        if (context == null) {
            throw new AssertionError("The method does not run on a Vert.x context");
        }
        if (!VertxContext.isOnDuplicatedContext()) {
            throw new AssertionError("The method does not run on a Vert.x **duplicated** context");
        }
    }

    public static void assertThatItRunsOnVirtualThread() {
        // We cannot depend on a Java 20.
        try {
            Method isVirtual = Thread.class.getMethod("isVirtual");
            isVirtual.setAccessible(true);
            boolean virtual = (Boolean) isVirtual.invoke(Thread.currentThread());
            if (!virtual) {
                throw new AssertionError("Thread " + Thread.currentThread() + " is not a virtual thread");
            }
        } catch (Exception e) {
            throw new AssertionError(
                    "Thread " + Thread.currentThread() + " is not a virtual thread - cannot invoke Thread.isVirtual()",
                    e);
        }
    }

    public static void assertNotOnVirtualThread() {
        // We cannot depend on a Java 20.
        try {
            Method isVirtual = Thread.class.getMethod("isVirtual");
            isVirtual.setAccessible(true);
            boolean virtual = (Boolean) isVirtual.invoke(Thread.currentThread());
            if (virtual) {
                throw new AssertionError("Thread " + Thread.currentThread() + " is a virtual thread");
            }
        } catch (Exception e) {
            // Trying using Thread name.
            var name = Thread.currentThread().toString();
            if (name.toLowerCase().contains("virtual")) {
                throw new AssertionError("Thread " + Thread.currentThread() + " seems to be a virtual thread");
            }
        }
    }
}
