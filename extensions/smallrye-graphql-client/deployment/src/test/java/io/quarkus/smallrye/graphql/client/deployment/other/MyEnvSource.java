package io.quarkus.smallrye.graphql.client.deployment.other;

import java.util.Map;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.EnvConfigSource;

@StaticInitSafe
public class MyEnvSource extends EnvConfigSource {
    public MyEnvSource() {
        super(Map.of("QUARKUS_SMALLRYE_GRAPHQL_CLIENT__KEY__URL", "http://localhost:8080/graphql", // runtime property
                "QUARKUS_SMALLRYE_GRAPHQL_CLIENT_ENABLE_BUILD_TIME_SCANNING", "FALSE"), ORDINAL); // build–time property
    }
}
