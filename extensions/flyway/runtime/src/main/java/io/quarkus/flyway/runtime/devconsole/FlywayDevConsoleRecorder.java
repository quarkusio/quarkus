package io.quarkus.flyway.runtime.devconsole;

import static java.util.List.of;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.flywaydb.core.Flyway;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.devconsole.runtime.spi.FlashScopeUtil.FlashMessageStatus;
import io.quarkus.flyway.runtime.FlywayBuildTimeConfig;
import io.quarkus.flyway.runtime.FlywayContainer;
import io.quarkus.flyway.runtime.FlywayContainersSupplier;
import io.quarkus.flyway.runtime.FlywayDataSourceBuildTimeConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class FlywayDevConsoleRecorder {

    public Handler<RoutingContext> datasourcesHandler() {
        return new DevConsolePostHandler() {
            @Override
            protected void handlePost(RoutingContext event, MultiMap form) {
                String datasource = form.get("datasource");
                String operation = form.get("operation");
                Collection<FlywayContainer> flywayContainers = new FlywayContainersSupplier().get();
                for (FlywayContainer flywayContainer : flywayContainers) {
                    if (flywayContainer.getDataSourceName().equals(datasource)) {
                        Flyway flyway = flywayContainer.getFlyway();
                        if ("clean".equals(operation)) {
                            flyway.clean();
                            flashMessage(event, "Database cleaned");
                            return;
                        } else if ("migrate".equals(operation)) {
                            flyway.migrate();
                            flashMessage(event, "Database migrated");
                            return;
                        } else {
                            flashMessage(event, "Invalid operation: " + operation, FlashMessageStatus.ERROR);
                            return;
                        }
                    }
                }
                flashMessage(event, "Datasource not found: " + datasource, FlashMessageStatus.ERROR);
            }
        };
    }

    public Handler<RoutingContext> createInitialMigrationHandler(FlywayBuildTimeConfig buildTimeConfig,
            String artifactId,
            Map<String, Supplier<String>> initialSqlSuppliers,
            // We can't interrogate these in the recorder because the config actually has defaults at that point
            boolean isBaselineOnMigrateConfigured,
            boolean isMigrateAtStartConfigured,
            boolean isCleanAtStartConfigured) {
        return new DevConsolePostHandler() {
            @Override
            protected void handlePostAsync(RoutingContext event, MultiMap form) throws Exception {
                String name = form.get("datasource");
                Supplier<String> found = initialSqlSuppliers.get(name);
                if (found == null) {
                    flashMessage(event, "Unable to find SQL generator");
                    return;
                }
                FlywayDataSourceBuildTimeConfig config = buildTimeConfig.getConfigForDataSourceName(name);
                if (config.locations.isEmpty()) {
                    flashMessage(event, "Datasource has no locations configured");
                    return;
                }

                List<Path> resourcesDir = DevConsoleManager.getHotReplacementContext().getResourcesDir();
                if (resourcesDir.isEmpty()) {
                    flashMessage(event, "No resource directory found");
                    return;
                }

                // In the current project only
                Path path = resourcesDir.get(0);

                Path migrationDir = path.resolve(config.locations.get(0));
                Files.createDirectories(migrationDir);
                Path file = migrationDir.resolve(
                        "V1.0.0__" + artifactId + ".sql");
                Files.writeString(file, found.get());
                flashMessage(event, file + " was created");
                Map<String, String> newConfig = new HashMap<>();
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
                flashMessage(event, "Initial migration created, Flyway will now manage this datasource");
                event.response().setStatusCode(HttpResponseStatus.SEE_OTHER.code()).headers()
                        .set(HttpHeaderNames.LOCATION,
                                event.request().absoluteURI().replace("create-initial-migration", "datasources"));
                event.response().end();
            }
        };
    }
}
