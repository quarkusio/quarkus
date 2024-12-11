package io.quarkus.flyway.deployment.devui;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.agroal.spi.JdbcInitialSQLGeneratorBuildItem;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.flyway.runtime.FlywayBuildTimeConfig;
import io.quarkus.flyway.runtime.dev.ui.FlywayDevUIRecorder;
import io.quarkus.flyway.runtime.dev.ui.FlywayJsonRpcService;

public class FlywayDevUIProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    @Record(value = RUNTIME_INIT, optional = true)
    CardPageBuildItem create(FlywayDevUIRecorder recorder, FlywayBuildTimeConfig buildTimeConfig,
            List<JdbcInitialSQLGeneratorBuildItem> generatorBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {

        Map<String, Supplier<String>> initialSqlSuppliers = new HashMap<>();
        for (JdbcInitialSQLGeneratorBuildItem buildItem : generatorBuildItem) {
            initialSqlSuppliers.put(buildItem.getDatabaseName(), buildItem.getSqlSupplier());
        }

        String artifactId = curateOutcomeBuildItem.getApplicationModel().getAppArtifact().getArtifactId();

        recorder.setInitialSqlSuppliers(initialSqlSuppliers, artifactId);

        CardPageBuildItem card = new CardPageBuildItem();

        card.addPage(Page.webComponentPageBuilder()
                .componentLink("qwc-flyway-datasources.js")
                .dynamicLabelJsonRPCMethodName("getNumberOfDatasources")
                .icon("font-awesome-solid:database"));
        return card;
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem registerJsonRpcBackend() {
        return new JsonRPCProvidersBuildItem(FlywayJsonRpcService.class);
    }
}
