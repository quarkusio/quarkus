package io.quarkus.grpc.examples.cancellation;

import org.junit.jupiter.api.Disabled;

import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@Disabled
@QuarkusTest
@TestProfile(VertxGRPCTestProfile.class)
public class VertxCancellationTest extends CancellationTest {
}
