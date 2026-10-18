package io.quarkus.grpc.examples.stork;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GrpcStorkResponseTimeCollectionTest extends GrpcStorkResponseTimeCollectionTestBase {

    @Test
    @Disabled("Disabled because of https://github.com/grpc/grpc-java/commit/8844cf7b87a04dd2d2e4a74cd0f0e3f4fed14113 which does not call the load balancer for each call anymore.")
    public void shouldCallConfigurableIfFaster() {

    }
}
