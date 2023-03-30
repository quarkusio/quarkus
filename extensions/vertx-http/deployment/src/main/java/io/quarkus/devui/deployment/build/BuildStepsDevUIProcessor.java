package io.quarkus.devui.deployment.build;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.devui.runtime.build.BuildStepsDevUIRecorder;
import io.quarkus.devui.runtime.build.BuildStepsJsonRPCService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;

@BuildSteps(onlyIf = { IsDevelopment.class })
public class BuildStepsDevUIProcessor {

    @BuildStep
    @Record(RUNTIME_INIT)
    public void create(BuildStepsDevUIRecorder recorder,
            BuildSystemTargetBuildItem buildSystemTarget) {
        recorder.setBuildMetricsPath(buildSystemTarget.getOutputDirectory().resolve("build-metrics.json").toString());
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem
                .builder()
                .addBeanClass(BuildStepsJsonRPCService.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem("build-steps", BuildStepsJsonRPCService.class);
    }
}
