package io.quarkus.smallrye.faulttolerance.deployment.devconsole;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.smallrye.faulttolerance.runtime.FaultToleranceOperationsDevConsoleSupplier;

public class FaultToleranceDevConsoleProcessor {
    @BuildStep(onlyIf = IsDevelopment.class)
    DevConsoleRuntimeTemplateInfoBuildItem collectInfo(CurateOutcomeBuildItem curateOutcome) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("ftOperations", new FaultToleranceOperationsDevConsoleSupplier(),
                this.getClass(), curateOutcome);
    }
}
