package io.quarkus.mongodb.deployment;

import java.util.List;

import com.mongodb.reactivestreams.client.ReactiveContextProvider;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Register additional {@link ReactiveContextProvider}s for the MongoDB clients.
 */
public final class ContextProviderBuildItem extends SimpleBuildItem {

    private final List<String> classNames;

    public ContextProviderBuildItem(List<String> classNames) {
        this.classNames = classNames == null ? List.of() : classNames;
    }

    public List<String> getContextProviderClassNames() {
        return classNames;
    }
}
