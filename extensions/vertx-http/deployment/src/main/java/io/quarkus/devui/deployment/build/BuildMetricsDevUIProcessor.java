package io.quarkus.devui.deployment.build;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.devui.runtime.build.BuildMetricsDevUIRecorder;
import io.quarkus.devui.runtime.build.BuildMetricsJsonRPCService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;

@BuildSteps(onlyIf = { IsDevelopment.class })
public class BuildMetricsDevUIProcessor {

    @BuildStep
    @Record(RUNTIME_INIT)
    public void create(BuildMetricsDevUIRecorder recorder,
            BuildSystemTargetBuildItem buildSystemTarget) {
        recorder.setBuildMetricsPath(buildSystemTarget.getOutputDirectory().resolve("build-metrics.json").toString());
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem("devui-build-metrics", BuildMetricsJsonRPCService.class);
    }
}
