package io.quarkus.grpc.test.utils;

import io.quarkus.test.junit.QuarkusTestProfile;

public class O2NGRPCTestProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "o2n"; // old Netty gRPC client --> new Vert.x gRPC server
    }
}
