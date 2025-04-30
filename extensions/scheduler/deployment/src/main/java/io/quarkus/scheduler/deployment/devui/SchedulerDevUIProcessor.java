package io.quarkus.scheduler.deployment.devui;

import java.util.List;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.FooterPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.WebComponentPageBuilder;
import io.quarkus.scheduler.deployment.ScheduledBusinessMethodItem;
import io.quarkus.scheduler.runtime.devui.SchedulerJsonRPCService;

public class SchedulerDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    void page(List<ScheduledBusinessMethodItem> scheduledMethods,
            BuildProducer<CardPageBuildItem> cardPages,
            BuildProducer<FooterPageBuildItem> footerPages) {

        CardPageBuildItem pageBuildItem = new CardPageBuildItem();

        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .icon("font-awesome-solid:clock")
                .componentLink("qwc-scheduler-scheduled-methods.js")
                .staticLabel(String.valueOf(scheduledMethods.size())));
        cardPages.produce(pageBuildItem);

        WebComponentPageBuilder logPageBuilder = Page.webComponentPageBuilder()
                .icon("font-awesome-solid:clock")
                .title("Scheduler")
                .componentLink("qwc-scheduler-log.js");
        footerPages.produce(new FooterPageBuildItem(logPageBuilder));
    }

    @BuildStep
    JsonRPCProvidersBuildItem rpcProvider() {
        return new JsonRPCProvidersBuildItem(SchedulerJsonRPCService.class);
    }

}
