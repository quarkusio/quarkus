package com.example.grpc.exc;

import io.quarkus.grpc.test.utils.N2OGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(N2OGRPCTestProfile.class)
public class N2OSmallryeHelloGrpcServiceTest extends SmallryeHelloGrpcServiceTestBase {
}
