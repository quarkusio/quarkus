package io.quarkus.grpc.auth;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class GrpcEagerAuthCustomRootPathTest extends GrpcAuthTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = createQuarkusUnitTest("""
            quarkus.grpc.server.use-separate-server=false
            quarkus.grpc.clients.securityClient.host=localhost
            quarkus.grpc.clients.securityClient.port=8081
            quarkus.http.root-path=/api
            """, true);

}
