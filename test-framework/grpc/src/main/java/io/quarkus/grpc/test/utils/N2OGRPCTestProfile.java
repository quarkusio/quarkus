package io.quarkus.grpc.test.utils;

import io.quarkus.test.junit.QuarkusTestProfile;

public class N2OGRPCTestProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "n2o"; // new Vert.x gRPC client --> old Netty gRPC server
    }
}
