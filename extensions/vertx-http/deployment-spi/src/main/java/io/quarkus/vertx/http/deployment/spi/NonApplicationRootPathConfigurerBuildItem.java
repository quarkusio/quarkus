package io.quarkus.vertx.http.deployment.spi;

import java.util.function.Consumer;

import io.quarkus.builder.item.MultiBuildItem;

public final class NonApplicationRootPathConfigurerBuildItem extends MultiBuildItem {
    private final Consumer<NonApplicationRootPathBuilder> configurer;

    public NonApplicationRootPathConfigurerBuildItem(Consumer<NonApplicationRootPathBuilder> configurer) {
        this.configurer = configurer;
    }

    public Consumer<NonApplicationRootPathBuilder> getConfigurer() {
        return configurer;
    }
}
