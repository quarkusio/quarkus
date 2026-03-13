package io.quarkus.agroal.deployment.devui;

import io.quarkus.agroal.runtime.DataSourcesJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.dev.shell.AgroalShellPage;
import io.quarkus.agroal.runtime.dev.ui.DatabaseInspector;
import io.quarkus.agroal.runtime.dev.ui.DatabaseInspectorRecorder;
import io.quarkus.assistant.runtime.dev.Assistant;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devshell.spi.ShellPageBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

@BuildSteps(onlyIf = IsLocalDevelopment.class)
class AgroalDevUIProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureDevUI(DataSourcesJdbcBuildTimeConfig config,
            DatabaseInspectorRecorder recorder,
            BuildProducer<CardPageBuildItem> cardPageProducer,
            LaunchModeBuildItem launchMode) {

        // Pass config values to runtime via recorder to avoid classloader issues
        recorder.setDevConfig(
                config.devui().allowSql(),
                config.devui().allowedDBHost().orElse(null));

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();
        cardPageBuildItem.setLogo("agroal_logo_dark.png", "agroal_logo_light.png");
        if (launchMode.getDevModeType().isPresent() && launchMode.getDevModeType().get().equals(DevModeType.LOCAL)) {
            if (config.devui().enabled()) {
                cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                        .icon("font-awesome-solid:database")
                        .title("Database view")
                        .componentLink("qwc-agroal-datasource.js"));
            }
        }

        cardPageProducer.produce(cardPageBuildItem);
    }

    @BuildStep
    void createBuildTimeActions(BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer) {
        BuildTimeActionBuildItem bta = new BuildTimeActionBuildItem();

        bta.actionBuilder()
                .methodName("generateMoreData")
                .assistantFunction((a, p) -> {
                    Assistant assistant = (Assistant) a;
                    return assistant.assistBuilder()
                            .userMessage(ADD_DATA_MESSAGE)
                            .variables(p)
                            .responseType(MoreDataResponse.class)
                            .assist();
                }).build();

        buildTimeActionProducer.produce(bta);

    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(DatabaseInspector.class);
    }

    @BuildStep
    ShellPageBuildItem createShellPage() {
        return ShellPageBuildItem.withCustomPage("Agroal", AgroalShellPage.class);
    }

    private static final String ADD_DATA_MESSAGE = """
            Given the provided sql script:
            {{currentInsertScript}}
            Can you add 10 more inserts into the script and return the result
            (including the provided entries, so update the script) in the script field.
            """;

    final record MoreDataResponse(String script) {
    }

}
