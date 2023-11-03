package io.quarkus.grpc.example.streaming;

import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;

@QuarkusTest
@TestProfile(VertxGRPCTestProfile.class)
@VirtualThreadUnit
@ShouldNotPin
public class VertxVirtualThreadTest extends VirtualThreadTestBase {
}
