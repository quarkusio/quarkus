package io.quarkus.grpc.examples.hello;

import io.quarkus.test.junit.QuarkusTestProfile;

public class VertxGrpcStorkContextDetachTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "vertx";
    }
}
