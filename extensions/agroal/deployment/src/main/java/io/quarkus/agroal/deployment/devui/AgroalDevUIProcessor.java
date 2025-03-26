package io.quarkus.agroal.deployment.devui;

import java.util.Optional;

import io.quarkus.agroal.runtime.DataSourcesJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.dev.ui.DatabaseInspector;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.dev.assistant.Assistant;
import io.quarkus.deployment.dev.assistant.AssistantBuildItem;
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
            BuildProducer<JsonRPCProvidersBuildItem> jsonRPCProviderProducer,
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
                jsonRPCProviderProducer.produce(new JsonRPCProvidersBuildItem(DatabaseInspector.class));
            }
        }
    }

    @BuildStep
    void createBuildTimeActions(Optional<AssistantBuildItem> assistantBuildItem,
            BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer) {

        if (assistantBuildItem.isPresent()) {
            BuildTimeActionBuildItem bta = new BuildTimeActionBuildItem();

            Assistant assistant = assistantBuildItem.get().getAssistant();

            // TODO: What if currentInsertScript is empty, maybe send table schema

            bta.addAction("generateMoreData", params -> {
                return assistant.assist(USER_MESSAGE, params);
            });

            buildTimeActionProducer.produce(bta);
        }
    }

    private static final String USER_MESSAGE = """
            Given the provided sql script:
            {{currentInsertScript}}

            Can you add 10 more inserts into the script and return the result
            (including the provided entries, so update the script)

            Return the result in a field called `script`.
            """;
}
