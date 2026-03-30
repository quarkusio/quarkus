package io.quarkus.grpc.auth;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class GrpcAuthTest extends GrpcAuthTestBase {

    @RegisterExtension
    static final QuarkusExtensionTest config = createQuarkusExtensionTest(null, true);

}
