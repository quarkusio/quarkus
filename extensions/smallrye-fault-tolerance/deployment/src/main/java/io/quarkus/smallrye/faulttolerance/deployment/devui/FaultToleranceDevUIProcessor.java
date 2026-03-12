package io.quarkus.smallrye.faulttolerance.deployment.devui;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devshell.spi.ShellPageBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.smallrye.faulttolerance.runtime.dev.shell.FaultToleranceShellPage;
import io.quarkus.smallrye.faulttolerance.runtime.devui.FaultToleranceJsonRpcService;

public class FaultToleranceDevUIProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    CardPageBuildItem cardPage(FaultToleranceInfoBuildItem faultToleranceInfo) {
        CardPageBuildItem pageBuildItem = new CardPageBuildItem();

        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Guarded Methods")
                .icon("font-awesome-solid:life-ring")
                .componentLink("qwc-fault-tolerance-methods.js")
                .staticLabel("" + faultToleranceInfo.getGuardedMethods()));

        return pageBuildItem;
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem jsonRPCService() {
        return new JsonRPCProvidersBuildItem(FaultToleranceJsonRpcService.class);
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    ShellPageBuildItem createShellPage() {
        return ShellPageBuildItem.withCustomPage(
                "Fault Tolerance",
                'F',
                FaultToleranceShellPage.class);
    }
}
