package io.quarkus.deployment.dev.testing;

import io.quarkus.builder.item.MultiBuildItem;

public final class TestListenerBuildItem extends MultiBuildItem {

    final TestListener listener;

    public TestListenerBuildItem(TestListener listener) {
        this.listener = listener;
    }

}
