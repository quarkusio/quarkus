package io.quarkus.grpc.examples.cancellation;

import org.junit.jupiter.api.Disabled;

import io.quarkus.grpc.test.utils.O2NGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@Disabled
@QuarkusTest
@TestProfile(O2NGRPCTestProfile.class)
public class O2NCancellationTest extends CancellationTest {
}
