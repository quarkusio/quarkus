package io.quarkus.flyway.runtime.dev.ui;

import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.flyway.runtime.FlywayRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FlywayDevUIRecorder {

    private final RuntimeValue<FlywayRuntimeConfig> runtimeConfig;

    public FlywayDevUIRecorder(RuntimeValue<FlywayRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public void initializeJsonRpcService(Map<String, Supplier<String>> initialSqlSuppliers,
            Map<String, Supplier<String>> updateSuppliers, String artifactId) {
        FlywayJsonRpcService rpcService = Arc.container().instance(FlywayJsonRpcService.class).get();
        rpcService.setInitialSqlSuppliers(initialSqlSuppliers);
        rpcService.setUpdateSqlSuppliers(updateSuppliers);
        rpcService.setArtifactId(artifactId);
        rpcService.setConfig(runtimeConfig.getValue());
    }

}
