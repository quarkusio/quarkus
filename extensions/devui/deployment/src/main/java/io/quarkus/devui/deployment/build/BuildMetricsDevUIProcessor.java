package io.quarkus.devui.deployment.build;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.devui.runtime.build.BuildMetricsDevUIRecorder;
import io.quarkus.devui.runtime.build.BuildMetricsJsonRPCService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;

@BuildSteps(onlyIf = { IsLocalDevelopment.class })
public class BuildMetricsDevUIProcessor {

    @BuildStep
    @Record(RUNTIME_INIT)
    public void create(BuildMetricsDevUIRecorder recorder,
            BuildSystemTargetBuildItem buildSystemTarget) {
        recorder.setBuildMetricsPath(buildSystemTarget.getOutputDirectory().resolve("build-metrics.json").toString());
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem
                .builder()
                .addBeanClass(BuildMetricsJsonRPCService.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem("devui-build-metrics", BuildMetricsJsonRPCService.class);
    }
}
