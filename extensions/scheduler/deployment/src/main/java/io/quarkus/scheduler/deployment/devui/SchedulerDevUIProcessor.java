package io.quarkus.scheduler.deployment.devui;

import java.util.List;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.scheduler.deployment.ScheduledBusinessMethodItem;
import io.quarkus.scheduler.runtime.devui.SchedulerJsonRPCService;

public class SchedulerDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem page(List<ScheduledBusinessMethodItem> scheduledMethods) {

        CardPageBuildItem pageBuildItem = new CardPageBuildItem("Scheduler");

        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .icon("font-awesome-solid:clock")
                .componentLink("qwc-scheduler-scheduled-methods.js")
                .staticLabel(String.valueOf(scheduledMethods.size())));

        return pageBuildItem;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem rpcProvider() {
        return new JsonRPCProvidersBuildItem("Scheduler", SchedulerJsonRPCService.class);
    }

}
