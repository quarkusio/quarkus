package io.quarkus.flyway.deployment.devui;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.agroal.spi.JdbcInitialSQLGeneratorBuildItem;
import io.quarkus.agroal.spi.JdbcUpdateSQLGeneratorBuildItem;
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
            List<JdbcInitialSQLGeneratorBuildItem> createGeneratorBuildItem,
            List<JdbcUpdateSQLGeneratorBuildItem> updateGeneratorBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {

        Map<String, Supplier<String>> initialSqlSuppliers = new HashMap<>();
        for (JdbcInitialSQLGeneratorBuildItem buildItem : createGeneratorBuildItem) {
            initialSqlSuppliers.put(buildItem.getDatabaseName(), buildItem.getSqlSupplier());
        }

        Map<String, Supplier<String>> updateSqlSuppliers = new HashMap<>();
        for (JdbcUpdateSQLGeneratorBuildItem buildItem : updateGeneratorBuildItem) {
            updateSqlSuppliers.put(buildItem.getDatabaseName(), buildItem.getSqlSupplier());
        }

        String artifactId = curateOutcomeBuildItem.getApplicationModel().getAppArtifact().getArtifactId();

        recorder.setSqlSuppliers(initialSqlSuppliers, updateSqlSuppliers, artifactId);

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
