package io.quarkus.flyway;

import static java.util.List.of;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.agroal.spi.JdbcInitialSQLGeneratorBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.flyway.runtime.FlywayBuildTimeConfig;
import io.quarkus.flyway.runtime.FlywayContainersSupplier;
import io.quarkus.flyway.runtime.FlywayDataSourceBuildTimeConfig;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public class FlywayDevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectBeanInfo(
            FlywayProcessor.MigrationStateBuildItem migrationStateBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("containers", new FlywayContainersSupplier());
    }

    @BuildStep
    DevConsoleRouteBuildItem invokeEndpoint(List<JdbcInitialSQLGeneratorBuildItem> generatorBuildItem,
            FlywayBuildTimeConfig buildTimeConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRouteBuildItem("create-initial-migration", "POST", new DevConsolePostHandler() {
            @Override
            protected void handlePostAsync(RoutingContext event, MultiMap form) throws Exception {
                String name = form.get("datasource");
                JdbcInitialSQLGeneratorBuildItem found = null;
                for (var i : generatorBuildItem) {
                    if (i.getDatabaseName().equals(name)) {
                        found = i;
                        break;
                    }
                }
                if (found == null) {
                    flashMessage(event, "Unable to find SQL generator");
                    return;
                }
                FlywayDataSourceBuildTimeConfig config = buildTimeConfig.getConfigForDataSourceName(name);
                if (config.locations.isEmpty()) {
                    flashMessage(event, "Datasource has no locations configured");
                    return;
                }
                System.out.println(found.getSqlSupplier().get());

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
                        "V1.0.0__" + curateOutcomeBuildItem.getEffectiveModel().getAppArtifact().getArtifactId() + ".sql");
                Files.writeString(file, found.getSqlSupplier().get());
                flashMessage(event, file + " was created");
                Map<String, String> newConfig = new HashMap<>();
                if (ConfigProvider.getConfig().getOptionalValue("quarkus.flyway.baseline-on-migrate", String.class).isEmpty()) {
                    newConfig.put("quarkus.flyway.baseline-on-migrate", "true");
                }
                if (ConfigProvider.getConfig().getOptionalValue("quarkus.flyway.migrate-at-start", String.class).isEmpty()) {
                    newConfig.put("quarkus.flyway.migrate-at-start", "true");
                }
                for (var profile : of("test", "dev")) {
                    if (ConfigProvider.getConfig().getOptionalValue("quarkus.flyway.clean-at-start", String.class).isEmpty()) {
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
        });
    }
}
