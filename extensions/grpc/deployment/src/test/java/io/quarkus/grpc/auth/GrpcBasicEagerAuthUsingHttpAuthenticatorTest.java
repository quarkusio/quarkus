package io.quarkus.grpc.auth;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class GrpcBasicEagerAuthUsingHttpAuthenticatorTest extends GrpcAuthTestBase {

    @RegisterExtension
    static final QuarkusExtensionTest config = createQuarkusExtensionTest("""
            quarkus.grpc.server.use-separate-server=false
            quarkus.grpc.clients.securityClient.host=localhost
            quarkus.grpc.clients.securityClient.port=8081
            """, false);

}
