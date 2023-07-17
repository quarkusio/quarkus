package io.quarkus.flyway;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.agroal.spi.JdbcInitialSQLGeneratorBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.flyway.runtime.FlywayBuildTimeConfig;
import io.quarkus.flyway.runtime.FlywayContainersSupplier;
import io.quarkus.flyway.runtime.devconsole.FlywayDevConsoleRecorder;
import io.quarkus.runtime.configuration.ConfigUtils;

public class FlywayDevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectBeanInfo(
            FlywayProcessor.MigrationStateBuildItem migrationStateBuildItem, CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("containers", new FlywayContainersSupplier(), this.getClass(),
                curateOutcomeBuildItem);
    }

    @BuildStep
    @Record(value = RUNTIME_INIT, optional = true)
    DevConsoleRouteBuildItem invokeEndpoint(FlywayDevConsoleRecorder recorder) {
        return new DevConsoleRouteBuildItem("datasources", "POST", recorder.datasourcesHandler());
    }

    @BuildStep
    @Record(value = RUNTIME_INIT, optional = true)
    DevConsoleRouteBuildItem invokeEndpoint(FlywayDevConsoleRecorder recorder,
            List<JdbcInitialSQLGeneratorBuildItem> generatorBuildItem,
            FlywayBuildTimeConfig buildTimeConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        Map<String, Supplier<String>> initialSqlSuppliers = new HashMap<>();
        for (JdbcInitialSQLGeneratorBuildItem buildItem : generatorBuildItem) {
            initialSqlSuppliers.put(buildItem.getDatabaseName(), buildItem.getSqlSupplier());
        }
        return new DevConsoleRouteBuildItem("create-initial-migration", "POST",
                recorder.createInitialMigrationHandler(buildTimeConfig,
                        curateOutcomeBuildItem.getApplicationModel().getAppArtifact().getArtifactId(), initialSqlSuppliers,
                        ConfigUtils.isPropertyPresent("quarkus.flyway.baseline-on-migrate"),
                        ConfigUtils.isPropertyPresent("quarkus.flyway.migrate-at-start"),
                        ConfigUtils.isPropertyPresent("quarkus.flyway.clean-at-start")));
    }
}
