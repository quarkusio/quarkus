package io.quarkus.agroal.deployment.devui;

import io.quarkus.agroal.runtime.DataSourcesJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.dev.ui.DatabaseInspector;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

@BuildSteps(onlyIf = IsLocalDevelopment.class)
class AgroalDevUIProcessor {

    @BuildStep
    void devUI(DataSourcesJdbcBuildTimeConfig config,
            BuildProducer<CardPageBuildItem> cardPageProducer,
            LaunchModeBuildItem launchMode) {

        if (launchMode.getDevModeType().isPresent() && launchMode.getDevModeType().get().equals(DevModeType.LOCAL)) {
            if (config.devui().enabled()) {
                CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

                cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                        .icon("font-awesome-solid:database")
                        .title("Database view")
                        .componentLink("qwc-agroal-datasource.js")
                        .metadata("allowSql", String.valueOf(config.devui().allowSql()))
                        .metadata("appendSql", config.devui().appendToDefaultSelect().orElse(""))
                        .metadata("allowedHost", config.devui().allowedDBHost().orElse(null)));

                cardPageProducer.produce(cardPageBuildItem);
            }
        }
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(DatabaseInspector.class);
    }
}
