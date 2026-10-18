package io.quarkus.deployment.dev.testing;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that holds a {@link TestListener} instance.
 * These listeners are notified of test events during development mode continuous testing.
 */
public final class TestListenerBuildItem extends MultiBuildItem {

    final TestListener listener;

    public TestListenerBuildItem(TestListener listener) {
        this.listener = listener;
    }

}
