package io.quarkus.grpc.auth;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class GrpcEagerAuthCustomRootPathTest extends GrpcAuthTestBase {

    @RegisterExtension
    static final QuarkusExtensionTest config = createQuarkusExtensionTest("""
            quarkus.grpc.server.use-separate-server=false
            quarkus.grpc.clients.securityClient.host=localhost
            quarkus.grpc.clients.securityClient.port=8081
            quarkus.http.root-path=/api
            """, true);

}
