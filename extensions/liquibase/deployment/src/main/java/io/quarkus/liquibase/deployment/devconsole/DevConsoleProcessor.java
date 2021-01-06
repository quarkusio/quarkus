package io.quarkus.liquibase.deployment.devconsole;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.liquibase.runtime.LiquibaseContainerSupplier;
import io.quarkus.liquibase.runtime.devconsole.LiquibaseDevConsoleRecorder;

public class DevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectBeanInfo() {
        return new DevConsoleRuntimeTemplateInfoBuildItem("containers", new LiquibaseContainerSupplier());
    }

    @BuildStep
    @Record(value = RUNTIME_INIT, optional = true)
    DevConsoleRouteBuildItem invokeEndpoint(LiquibaseDevConsoleRecorder recorder) {
        return new DevConsoleRouteBuildItem("containers", "POST", recorder.handler());
    }
}
