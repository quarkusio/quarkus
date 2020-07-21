package io.quarkus.mongodb.panache.config;

import static org.eclipse.microprofile.config.ConfigProvider.getConfig;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.mongodb.panache.MongoEntity;

public class MongoEntityConfigProvider {
    public static final String MONGODB_DEFAULT_DATABASE = "quarkus.mongodb.database";

    private static final String MONGODB_DEFAULT_CLIENT_NAME = "";
    private static final Map<String, String> databaseNameByClient = new ConcurrentHashMap<>();

    public static String getMongoDatabaseName(MongoEntity entity) {
        final String databaseName;
        if (entity != null && !entity.database().isEmpty()) {
            databaseName = entity.database();
        } else {
            String clientName = (entity == null) ? MONGODB_DEFAULT_CLIENT_NAME : entity.clientName();
            databaseName = getDatabaseName(clientName);
        }

        return databaseName;
    }

    private static String getDatabaseName(String clientName) {
        Objects.requireNonNull(clientName);
        return databaseNameByClient.computeIfAbsent(
                clientName,
                c -> getConfig()
                        .getOptionalValue(toPropertyName(c), String.class)
                        .orElseGet(() -> getConfig().getValue(MONGODB_DEFAULT_DATABASE, String.class)));
    }

    private static String toPropertyName(String clientName) {
        if (clientName == null || clientName.isEmpty()) {
            return MONGODB_DEFAULT_DATABASE;
        } else {
            return "quarkus.mongodb." + clientName + ".database";
        }
    }
}
