package io.quarkus.mongodb.deployment;

import java.util.List;

import com.mongodb.reactivestreams.client.ReactiveContextProvider;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Register additional {@link ReactiveContextProvider}s for the MongoDB clients.
 */
public final class ReactiveContextProviderBuildItem extends SimpleBuildItem {

    private final List<String> classNames;

    public ReactiveContextProviderBuildItem(List<String> classNames) {
        this.classNames = classNames;
    }

    public List<String> getReactiveContextProvidersClassNames() {
        return classNames;
    }
}
