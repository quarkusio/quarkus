package io.quarkus.grpc.auth;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class GrpcAuthTest extends GrpcAuthTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = createQuarkusUnitTest(null, true);

}
