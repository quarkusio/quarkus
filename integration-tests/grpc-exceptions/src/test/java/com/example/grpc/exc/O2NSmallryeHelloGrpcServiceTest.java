package com.example.grpc.exc;

import io.quarkus.grpc.test.utils.O2NGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(O2NGRPCTestProfile.class)
public class O2NSmallryeHelloGrpcServiceTest extends SmallryeHelloGrpcServiceTestBase {
}
