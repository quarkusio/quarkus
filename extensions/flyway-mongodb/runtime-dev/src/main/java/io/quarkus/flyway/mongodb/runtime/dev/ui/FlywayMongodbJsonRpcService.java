package io.quarkus.flyway.mongodb.runtime.dev.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.CleanResult;
import org.flywaydb.core.api.output.MigrateResult;

import io.quarkus.flyway.mongodb.runtime.FlywayMongodbContainer;
import io.quarkus.flyway.mongodb.runtime.FlywayMongodbContainerUtil;

public class FlywayMongodbJsonRpcService {

    public Collection<MongodbClient> getClients() {
        List<MongodbClient> result = new ArrayList<>();
        for (FlywayMongodbContainer container : FlywayMongodbContainerUtil.getActiveFlywayMongodbContainers()) {
            Flyway flyway = container.flyway();
            MigrationInfo current = flyway.info().current();
            // Baseline entries have a null version, so guard both the MigrationInfo and its version.
            String currentVersion = current != null && current.getVersion() != null
                    ? current.getVersion().getVersion()
                    : null;
            result.add(new MongodbClient(
                    container.clientName(),
                    container.hasMigrations(),
                    flyway.info().pending().length,
                    flyway.info().applied().length,
                    currentVersion,
                    container.resourceLocations(),
                    container.cleanDisabled()));
        }
        return result;
    }

    public int getNumberOfClients() {
        return FlywayMongodbContainerUtil.getActiveFlywayMongodbContainers().size();
    }

    public ActionResponse migrate(String client) {
        FlywayMongodbContainer container = FlywayMongodbContainerUtil.getFlywayMongodbContainer(client);
        if (container == null) {
            return new ActionResponse("error", "No Flyway MongoDB container found for client [" + client + "]");
        }
        try {
            MigrateResult result = container.flyway().migrate();
            if (result.success) {
                return new ActionResponse("success",
                        "Migrated " + result.migrationsExecuted + " script(s)");
            }
            return new ActionResponse("warning",
                    "Migration completed with warnings: " + result.warnings);
        } catch (Exception e) {
            return new ActionResponse("error", e.getMessage());
        }
    }

    public ActionResponse clean(String client) {
        FlywayMongodbContainer container = FlywayMongodbContainerUtil.getFlywayMongodbContainer(client);
        if (container == null) {
            return new ActionResponse("error", "No Flyway MongoDB container found for client [" + client + "]");
        }
        try {
            CleanResult result = container.flyway().clean();
            if (result.warnings == null || result.warnings.isEmpty()) {
                return new ActionResponse("success", "Cleaned " + result.schemasCleaned.size() + " schema(s)");
            }
            return new ActionResponse("warning", "Clean completed with warnings: " + result.warnings);
        } catch (Exception e) {
            return new ActionResponse("error", e.getMessage());
        }
    }

    public static final class MongodbClient {
        public final String name;
        public final boolean hasMigrations;
        public final int pendingCount;
        public final int appliedCount;
        public final String currentVersion;
        public final Set<String> resourceLocations;
        public final boolean cleanDisabled;

        public MongodbClient(String name, boolean hasMigrations, int pendingCount,
                int appliedCount, String currentVersion, Set<String> resourceLocations,
                boolean cleanDisabled) {
            this.name = name;
            this.hasMigrations = hasMigrations;
            this.pendingCount = pendingCount;
            this.appliedCount = appliedCount;
            this.currentVersion = currentVersion;
            this.resourceLocations = resourceLocations;
            this.cleanDisabled = cleanDisabled;
        }
    }

    public static final class ActionResponse {
        public final String type;
        public final String message;

        public ActionResponse(String type, String message) {
            this.type = type;
            this.message = message;
        }
    }
}
