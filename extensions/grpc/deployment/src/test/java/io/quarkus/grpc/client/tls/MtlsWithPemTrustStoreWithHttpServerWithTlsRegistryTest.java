package io.quarkus.grpc.client.tls;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "grpc", password = "password", formats = { Format.JKS, Format.PEM, Format.PKCS12 }, client = true)
})
class MtlsWithPemTrustStoreWithHttpServerWithTlsRegistryTest {

    private static final String configuration = """
            quarkus.tls.key-store.jks.path=target/certs/grpc-keystore.jks
            quarkus.tls.key-store.jks.password=password
            quarkus.tls.trust-store.jks.path=target/certs/grpc-server-truststore.jks
            quarkus.tls.trust-store.jks.password=password

            quarkus.tls.my-client.trust-store.pem.certs=target/certs/grpc-client-ca.crt
            quarkus.tls.my-client.key-store.pem.0.cert=target/certs/grpc-client.crt
            quarkus.tls.my-client.key-store.pem.0.key=target/certs/grpc-client.key

            quarkus.grpc.clients.hello.plain-text=false

            quarkus.grpc.clients.hello.tls-configuration-name=my-client
            quarkus.grpc.clients.hello.use-quarkus-grpc-client=true

            quarkus.grpc.server.use-separate-server=false
            quarkus.grpc.server.plain-text=false # Force the client to use TLS for the tests

            quarkus.http.ssl.client-auth=REQUIRED
            quarkus.http.insecure-requests=disabled
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(HelloWorldTlsEndpoint.class.getPackage())
                    .addPackage(GreeterGrpc.class.getPackage())
                    .add(new StringAsset(configuration), "application.properties"));

    @GrpcClient("hello")
    GreeterGrpc.GreeterBlockingStub blockingHelloService;

    @Test
    void testClientTlsConfiguration() {
        HelloReply reply = blockingHelloService.sayHello(HelloRequest.newBuilder().setName("neo").build());
        assertThat(reply.getMessage()).isEqualTo("Hello neo");
    }
}
