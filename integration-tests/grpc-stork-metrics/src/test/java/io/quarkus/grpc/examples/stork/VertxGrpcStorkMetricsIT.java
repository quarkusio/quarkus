package io.quarkus.grpc.examples.stork;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(VertxGrpcStorkMetricsTestProfile.class)
class VertxGrpcStorkMetricsIT extends VertxGrpcStorkMetricsTest {

}
