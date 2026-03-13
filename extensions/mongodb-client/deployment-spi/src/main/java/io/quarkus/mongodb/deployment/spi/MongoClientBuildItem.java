package io.quarkus.mongodb.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Registers a MongoDB client with a given name as a CDI bean.
 * <p>
 * Consumers looking for retrieving the list of Mongo clients should use {@link MongoClientsBuildItem} as consumers of
 * {@link MongoClientBuildItem} may contain duplicates.
 */
public final class MongoClientBuildItem extends MultiBuildItem {
    private static final String DEFAULT_CLIENT_NAME = "<default>";

    private final String name;
    private final boolean unremovable;

    MongoClientBuildItem(String name) {
        this(name, false);
    }

    MongoClientBuildItem(String name, boolean unremovable) {
        this.name = name;
        this.unremovable = unremovable;
    }

    public String getName() {
        return name;
    }

    public boolean isUnremovable() {
        return unremovable;
    }

    public boolean isDefault() {
        return DEFAULT_CLIENT_NAME.equals(name);
    }

    /**
     * Registers a MongoDB client with a given name as a CDI bean. The bean may be removed by Quarkus if no suitable
     * injection points for the client is found.
     *
     * @param name a {@code String} with the MongoDB client name
     * @return the {@link MongoClientBuildItem}
     */
    public static MongoClientBuildItem of(String name) {
        return new MongoClientBuildItem(name);
    }

    /**
     * Registers a MongoDB client with a given name as a CDI unremoveable bean.
     *
     * @param name a {@code String} with the MongoDB client name
     * @return the {@link MongoClientBuildItem}
     */
    public static MongoClientBuildItem ofUnremovable(String name) {
        return new MongoClientBuildItem(name, true);
    }

    /**
     * Registers the default MongoDB client as a CDI bean. The bean may be removed by Quarkus if no suitable
     * injection points for the client is found.
     *
     * @return the {@link MongoClientBuildItem}
     */
    public static MongoClientBuildItem defaultClient() {
        return new MongoClientBuildItem(DEFAULT_CLIENT_NAME);
    }

    /**
     * Registers the default MongoDB client as a CDI unremoveable bean.
     *
     * @return the {@link MongoClientBuildItem}
     */
    public static MongoClientBuildItem defaultClientUnremovable() {
        return new MongoClientBuildItem(DEFAULT_CLIENT_NAME, true);
    }
}
