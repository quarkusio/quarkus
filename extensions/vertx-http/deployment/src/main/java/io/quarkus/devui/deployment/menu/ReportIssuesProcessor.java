package io.quarkus.devui.deployment.menu;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.runtime.reportissues.ReportIssuesJsonRPCService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;

public class ReportIssuesProcessor {
    @BuildStep
    JsonRPCProvidersBuildItem registerJsonRpcService() {
        return new JsonRPCProvidersBuildItem("report-issues", ReportIssuesJsonRPCService.class);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    InternalPageBuildItem createReportIssuePage() {
        InternalPageBuildItem item = new InternalPageBuildItem("Report an Issue", 99);

        item.addPage(Page.webComponentPageBuilder().internal()
                .namespace("devui-report-issues")
                .title("Report an issue")
                .icon("font-awesome-solid:bug")
                .componentLink("qwc-report-issues.js"));

        return item;
    }
}
