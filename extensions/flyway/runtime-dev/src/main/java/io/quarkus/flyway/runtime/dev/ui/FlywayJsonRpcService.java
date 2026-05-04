package io.quarkus.flyway.runtime.dev.ui;

import static java.util.List.of;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.CleanResult;
import org.flywaydb.core.api.output.MigrateResult;

import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.flyway.runtime.FlywayContainer;
import io.quarkus.flyway.runtime.FlywayContainerUtil;
import io.quarkus.flyway.runtime.FlywayContainersSupplier;
import io.quarkus.flyway.runtime.FlywayRuntimeConfig;

public class FlywayJsonRpcService {

    private Map<String, Supplier<String>> initialSqlSuppliers;
    private Map<String, Supplier<String>> updateSqlSuppliers;
    private String artifactId;
    private Map<String, FlywayDatasource> datasources;
    private FlywayRuntimeConfig runtimeConfig;

    public void setInitialSqlSuppliers(Map<String, Supplier<String>> initialSqlSuppliers) {
        this.initialSqlSuppliers = initialSqlSuppliers;
    }

    public void setUpdateSqlSuppliers(Map<String, Supplier<String>> updateSqlSuppliers) {
        this.updateSqlSuppliers = updateSqlSuppliers;
    }

    public void setConfig(FlywayRuntimeConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public Collection<FlywayDatasource> getDatasources() {
        if (datasources == null) {
            datasources = new HashMap<>();
            Collection<FlywayContainer> flywayContainers = new FlywayContainersSupplier().get();
            for (FlywayContainer fc : flywayContainers) {
                var dsName = fc.getDataSourceName();
                datasources.put(fc.getDataSourceName(),
                        new FlywayDatasource(dsName, fc.isHasMigrations(), fc.isCreatePossible(),
                                runtimeConfig.datasources().get(dsName).cleanDisabled(),
                                fc.getResourceLocations()));
            }
        }
        return datasources.values();
    }

    public FlywayActionResponse clean(String ds) {
        Flyway flyway = getFlyway(ds);
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
        return errorNoDatasource(ds);
    }

    public FlywayActionResponse migrate(String ds) {
        Flyway flyway = getFlyway(ds);
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
        return errorNoDatasource(ds);
    }

    public FlywayActionResponse create(String ds) {
        this.getDatasources(); // Make sure we populated the datasources

        Supplier<String> found = initialSqlSuppliers.get(ds);
        if (found == null) {
            return new FlywayActionResponse("error", "Unable to find SQL generator");
        }

        String script = found.get();

        Flyway flyway = getFlyway(ds);

        if (flyway != null) {
            if (script != null) {
                try {
                    FlywayDatasource flywayDatasource = datasources.get(ds);
                    var locations = flywayDatasource.resourcesLocations;
                    if (locations.isEmpty()) {
                        return new FlywayActionResponse("error", "Datasource has no locations configured in Java resources");
                    }

                    List<Path> resourcesDir = DevConsoleManager.getHotReplacementContext().getResourcesDir();
                    if (resourcesDir.isEmpty()) {
                        return new FlywayActionResponse("error", "No resource directory found");
                    }

                    // In the current project only
                    Path path = resourcesDir.get(0);

                    Path migrationDir = path.resolve(locations.iterator().next());
                    Files.createDirectories(migrationDir);
                    Path file = migrationDir.resolve(
                            "V1.0.0__" + artifactId + ".sql");

                    Files.writeString(file, script);

                    flywayDatasource.hasMigrations = true;
                    flywayDatasource.createPossible = false;
                    var dsConfig = runtimeConfig.datasources().get(ds);
                    Map<String, String> newConfig = new HashMap<>();
                    if (dsConfig.baselineOnMigrate().isEmpty()) {
                        newConfig.put(FlywayContainerUtil.flywayPropertyKey(ds, "baseline-on-migrate"), "true");
                    }
                    if (dsConfig.migrateAtStart().isEmpty()) {
                        newConfig.put(FlywayContainerUtil.flywayPropertyKey(ds, "migrate-at-start"), "true");
                    }
                    for (var profile : of("test", "dev")) {
                        if (dsConfig.cleanAtStart().isEmpty()) {
                            newConfig.put("%" + profile + "." + FlywayContainerUtil.flywayPropertyKey(ds, "clean-at-start"),
                                    "true");
                        }
                    }
                    CurrentConfig.EDITOR.accept(newConfig);
                    // TODO ideally we'd force a scan here, but that fails because
                    //  we end up closing the JsonRpcService that is currently executing...
                    //  So for now we'll just rely on the next call to any API automatically triggering a restart.
                    //  Note the line of code below has never actually worked since
                    //  https://github.com/quarkusio/quarkus/commit/d68896ac48a0e4a6e95b60f3cb3af36e3bd98764#diff-b07030933573531c160a57f757c6949cb3d21dd4132f86656fac2e63413d24d8
                    //  because:
                    //  1. Adding a new Flyway file does not result in a restart; see https://github.com/quarkusio/quarkus/issues/25256
                    //  2. The automatic addition of config was broken until the commit that commented out the line below.
                    //  For ideas on how to fix this, see https://github.com/quarkusio/quarkus/pull/53884#discussion_r3162481517
                    // DevConsoleManager.getHotReplacementContext().doScan(true);
                    return new FlywayActionResponse("success",
                            "Initial migration created, Flyway will now manage this datasource");
                } catch (Throwable t) {
                    return new FlywayActionResponse("error", t.getMessage());
                }
            }
            return errorNoScript(ds);
        }
        return errorNoDatasource(ds);
    }

    public FlywayActionResponse update(String ds) {
        this.getDatasources(); // Make sure we populated the datasources

        try {
            Supplier<String> found = updateSqlSuppliers.get(ds);
            if (found == null) {
                return new FlywayActionResponse("error", "Unable to find SQL Update generator");
            }
            String script = found.get();
            if (script == null) {
                return new FlywayActionResponse("error", "Missing Flyway update script for [" + ds + "]");
            }
            Flyway flyway = getFlyway(ds);
            if (flyway == null) {
                return errorNoDatasource(ds);
            }
            FlywayDatasource flywayDatasource = datasources.get(ds);
            var locations = flywayDatasource.resourcesLocations;
            if (locations.isEmpty()) {
                return new FlywayActionResponse("error", "Datasource has no locations configured in Java resources");
            }

            List<Path> resourcesDir = DevConsoleManager.getHotReplacementContext().getResourcesDir();
            if (resourcesDir.isEmpty()) {
                return new FlywayActionResponse("error", "No resource directory found");
            }
            // In the current project only
            Path path = resourcesDir.get(0);

            Path migrationDir = path.resolve(locations.iterator().next());
            Files.createDirectories(migrationDir);
            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy.MM.dd.HHmmss");
            String timestamp = LocalDateTime.now().format(format);
            BigInteger major = flyway.info().current().isVersioned() ? flyway.info().current().getVersion().getMajor()
                    : BigInteger.ONE;
            Path file = migrationDir.resolve(
                    "V" + major + "." + timestamp + "__" + artifactId + ".sql");

            Files.writeString(file, script);

            return new FlywayActionResponse("success",
                    "migration created");

        } catch (Throwable t) {
            return new FlywayActionResponse("error", t.getMessage());
        }
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

    private Flyway getFlyway(String ds) {
        Collection<FlywayContainer> flywayContainers = new FlywayContainersSupplier().get();
        for (FlywayContainer flywayContainer : flywayContainers) {
            if (flywayContainer.getDataSourceName().equals(ds)) {
                return flywayContainer.getFlyway();
            }
        }
        return null;
    }

    public static final class FlywayDatasource {
        public final String name;
        public boolean hasMigrations;
        public boolean createPossible;
        public final boolean cleanDisabled;
        public final Set<String> resourcesLocations;

        public FlywayDatasource(String name, boolean hasMigrations, boolean createPossible, boolean cleanDisabled,
                Set<String> resourcesLocations) {
            this.name = name;
            this.hasMigrations = hasMigrations;
            this.createPossible = createPossible;
            this.cleanDisabled = cleanDisabled;
            this.resourcesLocations = resourcesLocations;
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
