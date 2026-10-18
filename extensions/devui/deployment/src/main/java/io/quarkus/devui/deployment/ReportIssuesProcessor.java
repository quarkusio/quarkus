package io.quarkus.devui.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.runtime.reportissues.ReportIssuesJsonRPCService;

public class ReportIssuesProcessor {
    @BuildStep
    JsonRPCProvidersBuildItem registerJsonRpcService() {
        return new JsonRPCProvidersBuildItem("report-issues", ReportIssuesJsonRPCService.class);
    }
}
