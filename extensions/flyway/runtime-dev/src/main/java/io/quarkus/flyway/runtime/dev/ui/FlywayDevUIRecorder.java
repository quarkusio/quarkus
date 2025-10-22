package io.quarkus.flyway.runtime.dev.ui;

import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FlywayDevUIRecorder {

    public RuntimeValue<Boolean> setSqlSuppliers(Map<String, Supplier<String>> initialSqlSuppliers,
            Map<String, Supplier<String>> updateSuppliers, String artifactId) {
        FlywayJsonRpcService rpcService = Arc.container().instance(FlywayJsonRpcService.class).get();
        rpcService.setInitialSqlSuppliers(initialSqlSuppliers);
        rpcService.setUpdateSqlSuppliers(updateSuppliers);
        rpcService.setArtifactId(artifactId);
        return new RuntimeValue<>(true);
    }

}
