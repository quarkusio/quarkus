package io.quarkus.grpc.example.streaming;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;

@QuarkusTest
@VirtualThreadUnit
@ShouldNotPin
public class VirtualThreadTest extends VirtualThreadTestBase {
}
