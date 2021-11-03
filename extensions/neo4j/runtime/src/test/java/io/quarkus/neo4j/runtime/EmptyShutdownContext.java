package io.quarkus.neo4j.runtime;

import io.quarkus.runtime.ShutdownContext;

/**
 * Only for tests, avoiding mocks.
 */
final class EmptyShutdownContext implements ShutdownContext {
    @Override
    public void addShutdownTask(Runnable runnable) {
    }

    @Override
    public void addLastShutdownTask(Runnable runnable) {
    }
}
