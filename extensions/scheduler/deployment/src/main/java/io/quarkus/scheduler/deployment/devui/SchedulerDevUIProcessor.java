package io.quarkus.scheduler.deployment.devui;

import java.util.List;

import io.quarkus.assistant.runtime.dev.Assistant;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.FooterPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.WebComponentPageBuilder;
import io.quarkus.scheduler.deployment.ScheduledBusinessMethodItem;
import io.quarkus.scheduler.runtime.dev.ui.SchedulerJsonRPCService;

public class SchedulerDevUIProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void page(List<ScheduledBusinessMethodItem> scheduledMethods,
            BuildProducer<CardPageBuildItem> cardPages,
            BuildProducer<FooterPageBuildItem> footerPages) {

        CardPageBuildItem pageBuildItem = new CardPageBuildItem();

        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .icon("font-awesome-solid:clock")
                .componentLink("qwc-scheduler-scheduled-methods.js")
                .staticLabel(String.valueOf(scheduledMethods.size())));

        pageBuildItem.addPage(Page.assistantPageBuilder()
                .title("Cron Builder")
                .componentLink("qwc-scheduler-cron-builder.js"));

        cardPages.produce(pageBuildItem);

        WebComponentPageBuilder logPageBuilder = Page.webComponentPageBuilder()
                .icon("font-awesome-solid:clock")
                .title("Scheduler")
                .componentLink("qwc-scheduler-log.js");
        footerPages.produce(new FooterPageBuildItem(logPageBuilder));
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void createBuildTimeActions(BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer) {
        BuildTimeActionBuildItem bta = new BuildTimeActionBuildItem();

        bta.actionBuilder()
                .methodName("interpretCron")
                .assistantFunction((a, p) -> {
                    Assistant assistant = (Assistant) a;

                    return assistant.assistBuilder()
                            .userMessage(INTERPRET_CRON)
                            .variables(p)
                            .assist();
                }).build();

        bta.actionBuilder()
                .methodName("createCron")
                .assistantFunction((a, p) -> {
                    Assistant assistant = (Assistant) a;

                    return assistant.assistBuilder()
                            .userMessage(CREATE_CRON)
                            .variables(p)
                            .assist();
                }).build();

        buildTimeActionProducer.produce(bta);
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem rpcProvider() {
        return new JsonRPCProvidersBuildItem(SchedulerJsonRPCService.class);
    }

    private static final String INTERPRET_CRON = """
            Can you please interpret this cron and describe it in plain English. Reply in markdown format in a field called markdown.

            Here is the cron: {{cron}}
            """;

    private static final String CREATE_CRON = """
            Can you create a valid cron expression for the following description: {{description}}

            Reply with the valid cron in a field called cron. Add an example on how to use this with the quarkus-scheduler extenion in a field called example. For the example please use markdown.
            """;

}
