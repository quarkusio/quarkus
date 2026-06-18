package io.quarkus.reactive.datasource.spi;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Declares a type that is injectable as a reactive datasource bean.
 * <p>
 * Produced by each reactive SQL client extension (PG, MySQL, etc.)
 * to let the common reactive datasource processor scan for injection points of those types.
 */
public final class ReactiveDataSourceInjectableTypeBuildItem extends MultiBuildItem {

    private final DotName typeName;

    public ReactiveDataSourceInjectableTypeBuildItem(DotName typeName) {
        this.typeName = typeName;
    }

    public DotName getTypeName() {
        return typeName;
    }
}
