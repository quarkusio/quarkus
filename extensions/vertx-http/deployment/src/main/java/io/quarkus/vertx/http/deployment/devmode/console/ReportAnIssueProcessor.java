package io.quarkus.vertx.http.deployment.devmode.console;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.runtime.ReportAnIssueJsonRPCService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;

public class ReportAnIssueProcessor {
    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem registerJsonRpcService() {
        return new JsonRPCProvidersBuildItem("report-an-issue", ReportAnIssueJsonRPCService.class);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    InternalPageBuildItem createBuildMetricsPages() {
        InternalPageBuildItem item = new InternalPageBuildItem("Report an Issue", 99);

        item.addPage(Page.webComponentPageBuilder().internal()
                .namespace("devui-report-issues")
                .title("Report an issue")
                .icon("font-awesome-solid:bug")
                .componentLink("qwc-report-issues.js"));

        return item;
    }
}