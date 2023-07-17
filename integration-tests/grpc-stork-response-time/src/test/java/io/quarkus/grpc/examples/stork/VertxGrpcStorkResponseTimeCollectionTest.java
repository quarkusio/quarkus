package io.quarkus.grpc.examples.stork;

import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(VertxGRPCTestProfile.class)
class VertxGrpcStorkResponseTimeCollectionTest extends GrpcStorkResponseTimeCollectionTestBase {
}
