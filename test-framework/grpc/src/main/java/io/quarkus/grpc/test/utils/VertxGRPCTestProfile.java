package io.quarkus.grpc.test.utils;

import io.quarkus.test.junit.QuarkusTestProfile;

public class VertxGRPCTestProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "vertx";
    }
}
