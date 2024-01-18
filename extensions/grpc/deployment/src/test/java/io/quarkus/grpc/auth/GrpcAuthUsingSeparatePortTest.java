package io.quarkus.grpc.auth;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class GrpcAuthUsingSeparatePortTest extends GrpcAuthTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = createQuarkusUnitTest("quarkus.grpc.server.use-separate-server=false\n" +
            "quarkus.grpc.clients.securityClient.host=localhost\n" +
            "quarkus.grpc.clients.securityClient.port=8081\n", true);

}
