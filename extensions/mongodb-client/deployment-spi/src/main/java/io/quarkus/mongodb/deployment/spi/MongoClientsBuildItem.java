package io.quarkus.mongodb.deployment.spi;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The unique list of MongoDB client names.
 */
public final class MongoClientsBuildItem extends SimpleBuildItem {
    private final List<MongoClientBuildItem> mongoClients;

    MongoClientsBuildItem(List<MongoClientBuildItem> mongoClients) {
        this.mongoClients = mongoClients;
    }

    public List<MongoClientBuildItem> getMongoClients() {
        return mongoClients;
    }

    /**
     * Creates the unique list of MongoDB clients. Clients with the same name are merged, and a client type is
     * unremovable if any of the merged clients marks it as unremovable.
     *
     * @param mongoClients the {@link MongoClientBuildItem}, which may contain duplicates
     * @return the {@link MongoClientsBuildItem}
     */
    public static MongoClientsBuildItem of(List<MongoClientBuildItem> mongoClients) {
        Map<String, Set<MongoClientBuildItem.Type>> unremovableTypes = new LinkedHashMap<>();
        for (MongoClientBuildItem mongoClient : mongoClients) {
            unremovableTypes.computeIfAbsent(mongoClient.getName(), k -> EnumSet.noneOf(MongoClientBuildItem.Type.class))
                    .addAll(mongoClient.getUnremovableTypes());
        }
        return new MongoClientsBuildItem(unremovableTypes.entrySet().stream()
                .map(entry -> new MongoClientBuildItem(entry.getKey(), entry.getValue()))
                .toList());
    }
}
