package io.quarkus.agroal.deployment.devui;

import io.quarkus.agroal.runtime.DataSourcesJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.dev.ui.DatabaseInspector;
import io.quarkus.assistant.runtime.dev.Assistant;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

@BuildSteps(onlyIf = IsLocalDevelopment.class)
class AgroalDevUIProcessor {

    @BuildStep
    void devUI(DataSourcesJdbcBuildTimeConfig config,
            BuildProducer<CardPageBuildItem> cardPageProducer,
            LaunchModeBuildItem launchMode) {

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();
        cardPageBuildItem.setLogo("agroal_logo_dark.png", "agroal_logo_light.png");
        if (launchMode.getDevModeType().isPresent() && launchMode.getDevModeType().get().equals(DevModeType.LOCAL)) {
            if (config.devui().enabled()) {
                cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                        .icon("font-awesome-solid:database")
                        .title("Database view")
                        .componentLink("qwc-agroal-datasource.js")
                        .metadata("allowSql", String.valueOf(config.devui().allowSql()))
                        .metadata("appendSql", config.devui().appendToDefaultSelect().orElse(""))
                        .metadata("allowedHost", config.devui().allowedDBHost().orElse(null)));
            }
        }

        cardPageProducer.produce(cardPageBuildItem);
    }

    @BuildStep
    void createBuildTimeActions(BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer) {
        BuildTimeActionBuildItem bta = new BuildTimeActionBuildItem();

        // TODO: If currentInsertScript is empty, maybe send tables schema. This might mean we need to move this to runtime

        bta.addAssistantAction("generateMoreData", (a, p) -> {
            Assistant assistant = (Assistant) a;
            return assistant.assistBuilder()
                    .userMessage(USER_MESSAGE)
                    .variables(p)
                    .assist();
        });
        buildTimeActionProducer.produce(bta);
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(DatabaseInspector.class);
    }

    private static final String USER_MESSAGE = """
            Given the provided sql script:
            {{currentInsertScript}}
            Can you add 10 more inserts into the script and return the result
            (including the provided entries, so update the script)
            Return the result in a field called `script`.
            """;
}
