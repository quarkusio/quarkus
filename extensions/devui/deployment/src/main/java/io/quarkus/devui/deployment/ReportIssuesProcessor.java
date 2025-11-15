package io.quarkus.devui.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.runtime.reportissues.ReportIssuesJsonRPCService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;

public class ReportIssuesProcessor {
    @BuildStep
    JsonRPCProvidersBuildItem registerJsonRpcService() {
        return new JsonRPCProvidersBuildItem("report-issues", ReportIssuesJsonRPCService.class);
    }
}
