package io.quarkus.amazon.lambda.deployment;

import java.util.function.Supplier;

import io.quarkus.amazon.lambda.runtime.MockEventServer;
import io.quarkus.builder.item.SimpleBuildItem;

public final class EventServerOverrideBuildItem extends SimpleBuildItem {
    private Supplier<MockEventServer> server;

    public EventServerOverrideBuildItem(Supplier<MockEventServer> server) {
        this.server = server;
    }

    public Supplier<MockEventServer> getServer() {
        return server;
    }
}
