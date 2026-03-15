package io.quarkus.mongodb.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.mongodb.MongoClientName;

/**
 * Represents the values of the {@link MongoClientName}.
 *
 * @deprecated use {@link io.quarkus.mongodb.deployment.spi.MongoClientBuildItem} instead.
 */
@Deprecated(forRemoval = true, since = "3.33")
public final class MongoClientNameBuildItem extends MultiBuildItem {

    private final String name;
    private final boolean addQualifier;

    public MongoClientNameBuildItem(String name) {
        this(name, true);
    }

    /**
     * @deprecated use {@link io.quarkus.mongodb.deployment.spi.MongoClientBuildItem} instead.
     *             {@code addQualifier} has no effect.
     */
    @Deprecated(forRemoval = true, since = "3.33")
    public MongoClientNameBuildItem(String name, boolean addQualifier) {
        this.name = name;
        this.addQualifier = addQualifier;
    }

    public String getName() {
        return name;
    }

    /**
     * @deprecated {@code addQualifier} has no effect.
     */
    @Deprecated(forRemoval = true, since = "3.33")
    public boolean isAddQualifier() {
        return addQualifier;
    }
}
