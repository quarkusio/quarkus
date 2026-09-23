package io.quarkus.grpc.examples.stork;

import io.quarkus.test.junit.QuarkusTestProfile;

public class VertxGrpcStorkMetricsTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "vertx";
    }
}
