package io.quarkus.mongodb.deployment.spi;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

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
    private final Set<Type> unremovableTypes;

    MongoClientBuildItem(String name) {
        this(name, EnumSet.noneOf(Type.class));
    }

    MongoClientBuildItem(String name, Set<Type> unremovableTypes) {
        this.name = name;
        this.unremovableTypes = Collections.unmodifiableSet(EnumSet.copyOf(unremovableTypes));
    }

    public String getName() {
        return name;
    }

    /**
     * @return {@code true} if at least one of the client types is unremovable
     */
    public boolean isUnremovable() {
        return !unremovableTypes.isEmpty();
    }

    /**
     * @param type the client type
     * @return {@code true} if the client type is unremovable
     */
    public boolean isUnremovable(Type type) {
        return unremovableTypes.contains(type);
    }

    Set<Type> getUnremovableTypes() {
        return unremovableTypes;
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
     * Registers a MongoDB client with a given name as a CDI unremoveable bean, for both the blocking and the reactive
     * client types.
     *
     * @param name a {@code String} with the MongoDB client name
     * @return the {@link MongoClientBuildItem}
     */
    public static MongoClientBuildItem ofUnremovable(String name) {
        return new MongoClientBuildItem(name, EnumSet.allOf(Type.class));
    }

    /**
     * Registers a MongoDB client with a given name as a CDI unremoveable bean, only for the given client type. The
     * other client type is only registered if suitable injection points for it are found.
     * <p>
     * The blocking and the reactive clients are separate driver clients, each with its own connection pool, so
     * consumers that only use one of the client types should prefer this method over {@link #ofUnremovable(String)}.
     *
     * @param name a {@code String} with the MongoDB client name
     * @param type the client {@link Type} to register as unremovable
     * @return the {@link MongoClientBuildItem}
     */
    public static MongoClientBuildItem ofUnremovable(String name, Type type) {
        return new MongoClientBuildItem(name, EnumSet.of(type));
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
     * Registers the default MongoDB client as a CDI unremoveable bean, for both the blocking and the reactive client
     * types.
     *
     * @return the {@link MongoClientBuildItem}
     */
    public static MongoClientBuildItem defaultClientUnremovable() {
        return new MongoClientBuildItem(DEFAULT_CLIENT_NAME, EnumSet.allOf(Type.class));
    }

    /**
     * Registers the default MongoDB client as a CDI unremoveable bean, only for the given client type. The other
     * client type is only registered if suitable injection points for it are found.
     *
     * @param type the client {@link Type} to register as unremovable
     * @return the {@link MongoClientBuildItem}
     * @see #ofUnremovable(String, Type)
     */
    public static MongoClientBuildItem defaultClientUnremovable(Type type) {
        return new MongoClientBuildItem(DEFAULT_CLIENT_NAME, EnumSet.of(type));
    }

    /**
     * The MongoDB client types.
     */
    public enum Type {
        /**
         * The blocking client, {@code com.mongodb.client.MongoClient}.
         */
        BLOCKING,
        /**
         * The reactive client, {@code io.quarkus.mongodb.reactive.ReactiveMongoClient}.
         */
        REACTIVE
    }
}
