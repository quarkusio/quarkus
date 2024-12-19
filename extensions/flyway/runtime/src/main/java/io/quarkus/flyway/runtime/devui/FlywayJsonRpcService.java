package io.quarkus.flyway.runtime.devui;

import static java.util.List.of;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.CleanResult;
import org.flywaydb.core.api.output.MigrateResult;

import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.flyway.runtime.FlywayContainer;
import io.quarkus.flyway.runtime.FlywayContainersSupplier;
import io.quarkus.runtime.configuration.ConfigUtils;

public class FlywayJsonRpcService {

    private Map<String, Supplier<String>> initialSqlSuppliers;
    private String artifactId;
    private Map<String, FlywayDatasource> datasources;

    @ConfigProperty(name = "quarkus.flyway.locations")
    private List<String> locations;

    public void setInitialSqlSuppliers(Map<String, Supplier<String>> initialSqlSuppliers) {
        this.initialSqlSuppliers = initialSqlSuppliers;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public Collection<FlywayDatasource> getDatasources() {
        if (datasources == null) {
            datasources = new HashMap<>();
            Collection<FlywayContainer> flywayContainers = new FlywayContainersSupplier().get();
            for (FlywayContainer fc : flywayContainers) {
                datasources.put(fc.getId(),
                        new FlywayDatasource(fc.getDataSourceName(), fc.getName(), fc.getId(), fc.isHasMigrations(),
                                fc.isCreatePossible()));
            }
        }
        return datasources.values();
    }

    public FlywayActionResponse clean(String id) {
        Flyway flyway = getFlyway(id);
        if (flyway != null) {
            CleanResult cleanResult = flyway.clean();
            if (cleanResult.warnings != null && cleanResult.warnings.size() > 0) {
                return new FlywayActionResponse("warning",
                        "Cleaning failed",
                        cleanResult.warnings.size(),
                        null,
                        cleanResult.database, cleanResult.warnings);
            } else {
                return new FlywayActionResponse("success",
                        "Cleaned",
                        cleanResult.schemasCleaned.size(),
                        null,
                        cleanResult.database);
            }

        }
        return errorNoDatasource(id);
    }

    public FlywayActionResponse migrate(String id) {
        Flyway flyway = getFlyway(id);
        if (flyway != null) {
            MigrateResult migrateResult = flyway.migrate();
            if (migrateResult.success) {
                return new FlywayActionResponse("success",
                        "Migration executed",
                        migrateResult.migrationsExecuted,
                        migrateResult.schemaName,
                        migrateResult.database);
            } else {
                return new FlywayActionResponse("warning",
                        "Migration failed",
                        migrateResult.warnings.size(),
                        migrateResult.schemaName,
                        migrateResult.database,
                        migrateResult.warnings);
            }
        }
        return errorNoDatasource(id);
    }

    public FlywayActionResponse create(String id) {
        this.getDatasources(); // Make sure we populated the datasources

        FlywayDatasource flywayDatasource = datasources.get(id);
        if (flywayDatasource == null) {
            return new FlywayActionResponse("error", "flyway with id " + id + " not found");
        }
        Supplier<String> found = initialSqlSuppliers.get(flywayDatasource.datasourceName);
        if (found == null) {
            return new FlywayActionResponse("error", "Unable to find SQL generator");
        }

        String script = found.get();

        Flyway flyway = getFlyway(id);
        if (flyway != null) {
            if (script != null) {
                Map<String, String> params = Map.of("ds", id, "script", script, "artifactId", artifactId);
                try {
                    if (locations.isEmpty()) {
                        return new FlywayActionResponse("error", "Datasource has no locations configured");
                    }

                    List<Path> resourcesDir = DevConsoleManager.getHotReplacementContext().getResourcesDir();
                    if (resourcesDir.isEmpty()) {
                        return new FlywayActionResponse("error", "No resource directory found");
                    }

                    // In the current project only
                    Path path = resourcesDir.get(0);

                    Path migrationDir = path.resolve(locations.get(0));
                    Files.createDirectories(migrationDir);
                    Path file = migrationDir.resolve(
                            "V1.0.0__" + artifactId + ".sql");

                    Files.writeString(file, script);

                    flywayDatasource.hasMigrations = true;
                    flywayDatasource.createPossible = false;
                    Map<String, String> newConfig = new HashMap<>();
                    boolean isBaselineOnMigrateConfigured = ConfigUtils
                            .isPropertyPresent("quarkus.flyway.baseline-on-migrate");
                    boolean isMigrateAtStartConfigured = ConfigUtils.isPropertyPresent("quarkus.flyway.migrate-at-start");
                    boolean isCleanAtStartConfigured = ConfigUtils.isPropertyPresent("quarkus.flyway.clean-at-start");
                    if (!isBaselineOnMigrateConfigured) {
                        newConfig.put("quarkus.flyway.baseline-on-migrate", "true");
                    }
                    if (!isMigrateAtStartConfigured) {
                        newConfig.put("quarkus.flyway.migrate-at-start", "true");
                    }
                    for (var profile : of("test", "dev")) {
                        if (!isCleanAtStartConfigured) {
                            newConfig.put("%" + profile + ".quarkus.flyway.clean-at-start", "true");
                        }
                    }
                    CurrentConfig.EDITOR.accept(newConfig);
                    //force a scan, to make sure everything is up-to-date
                    DevConsoleManager.getHotReplacementContext().doScan(true);
                    return new FlywayActionResponse("success",
                            "Initial migration created, Flyway will now manage this datasource");
                } catch (Throwable t) {
                    return new FlywayActionResponse("error", t.getMessage());
                }
            }
            return errorNoScript(id);
        }
        return errorNoDatasource(id);
    }

    public int getNumberOfDatasources() {
        Collection<FlywayContainer> flywayContainers = new FlywayContainersSupplier().get();
        return flywayContainers.size();
    }

    private FlywayActionResponse errorNoDatasource(String ds) {
        return new FlywayActionResponse("error", "Flyway datasource not found [" + ds + "]");
    }

    private FlywayActionResponse errorNoScript(String ds) {
        return new FlywayActionResponse("error", "Missing Flyway initial script for [" + ds + "]");
    }

    private Flyway getFlyway(String id) {
        Collection<FlywayContainer> flywayContainers = new FlywayContainersSupplier().get();
        for (FlywayContainer flywayContainer : flywayContainers) {
            if (flywayContainer.getId().equals(id)) {
                return flywayContainer.getFlyway();
            }
        }
        return null;
    }

    public static class FlywayDatasource {
        public String datasourceName;
        public String name;
        public String id;
        public boolean hasMigrations;
        public boolean createPossible;

        public FlywayDatasource() {
        }

        public FlywayDatasource(String datasourceName, String name, String id, boolean hasMigrations, boolean createPossible) {
            this.datasourceName = datasourceName;
            this.name = name;
            this.id = id;
            this.hasMigrations = hasMigrations;
            this.createPossible = createPossible;
        }
    }

    public static class FlywayActionResponse {
        public String type;
        public String message;
        public int number;
        public String schema;
        public String database;
        public List<String> warnings;

        public FlywayActionResponse() {
        }

        public FlywayActionResponse(String type, String message) {
            this(type, message, -1, null, null, List.of());
        }

        public FlywayActionResponse(String type, String message, int number, String schema, String database) {
            this(type, message, number, schema, database, List.of());
        }

        public FlywayActionResponse(String type, String message, int number, String schema, String database,
                List<String> warnings) {
            this.type = type;
            this.message = message;
            this.number = number;
            this.schema = schema;
            this.database = database;
            this.warnings = warnings;
        }
    }
}
