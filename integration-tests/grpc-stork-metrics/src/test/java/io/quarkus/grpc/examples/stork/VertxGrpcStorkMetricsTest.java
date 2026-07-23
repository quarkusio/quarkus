package io.quarkus.grpc.examples.stork;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(VertxGrpcStorkMetricsTestProfile.class)
class VertxGrpcStorkMetricsTest extends GrpcStorkMetricsTestBase {

}
