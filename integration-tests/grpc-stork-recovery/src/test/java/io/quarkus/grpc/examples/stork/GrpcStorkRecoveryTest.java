package io.quarkus.grpc.examples.stork;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Recovery test for the classic Java gRPC client (the default,
 * {@code use-quarkus-grpc-client=false}). The {@code stork://} channel is
 * resolved by {@code GrpcStorkServiceDiscovery}.
 */
@QuarkusTest
class GrpcStorkRecoveryTest extends GrpcStorkRecoveryTestBase {

}
