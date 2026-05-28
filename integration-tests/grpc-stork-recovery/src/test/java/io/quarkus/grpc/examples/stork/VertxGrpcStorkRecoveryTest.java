package io.quarkus.grpc.examples.stork;

import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * Recovery test for the Vert.x gRPC client ({@code use-quarkus-grpc-client=true}),
 * which resolves {@code stork://} through {@code StorkGrpcChannel} instead of a
 * gRPC {@code NameResolver}. Before the fix, {@code StorkGrpcChannel} delivered a
 * failed call's close callback on the channel executor instead of the call's own
 * executor, so a blocking stub call hung instead of failing fast and the client
 * never got the chance to retry.
 */
@QuarkusTest
@TestProfile(VertxGRPCTestProfile.class)
class VertxGrpcStorkRecoveryTest extends GrpcStorkRecoveryTestBase {

}
