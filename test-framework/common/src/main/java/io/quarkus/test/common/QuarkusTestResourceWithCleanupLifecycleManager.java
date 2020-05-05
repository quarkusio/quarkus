package io.quarkus.test.common;

public interface QuarkusTestResourceWithCleanupLifecycleManager extends QuarkusTestResourceLifecycleManager {

    default void cleanup() {
    }
}
