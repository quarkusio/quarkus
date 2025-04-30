package io.quarkus.grpc.server.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.ManagedChannel;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "grpc", password = "password", formats = { Format.JKS, Format.PEM, Format.PKCS12 }, client = true)
})
public class TlsWithHttpServerUsingP12Test {

    static String configuration = """
            quarkus.grpc.server.use-separate-server=false

            quarkus.http.ssl.certificate.key-store-file=target/certs/grpc-keystore.p12
            quarkus.http.ssl.certificate.key-store-password=password
            quarkus.http.insecure-requests=disabled
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClass(HelloService.class)
                    .add(new StringAsset(configuration), "application.properties"));

    protected ManagedChannel channel;

    @BeforeEach
    public void init() throws Exception {
        File certs = new File("target/certs/grpc-client-ca.crt");
        SslContext sslcontext = GrpcSslContexts.forClient()
                .trustManager(certs)
                .build();
        channel = NettyChannelBuilder.forAddress("localhost", 8444)
                .sslContext(sslcontext)
                .useTransportSecurity()
                .build();
    }

    @AfterEach
    public void shutdown() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    @Test
    public void testInvokingGrpcServiceUsingTls() {
        HelloReply reply = GreeterGrpc.newBlockingStub(channel)
                .sayHello(HelloRequest.newBuilder().setName("neo").build());
        assertThat(reply.getMessage()).isEqualTo("Hello neo");
    }

}
