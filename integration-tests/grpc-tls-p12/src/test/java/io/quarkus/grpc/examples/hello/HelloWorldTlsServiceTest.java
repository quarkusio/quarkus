package io.quarkus.grpc.examples.hello;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.grpc.Channel;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientChannel;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "grpc-tls", password = "wibble", formats = {
        Format.PKCS12, Format.PEM }))
@QuarkusTest
class HelloWorldTlsServiceTest {

    Channel channel;

    private GrpcClient client;

    @Inject
    Vertx vertx;

    @BeforeEach
    public void init() throws Exception {
        HttpClientOptions options = new HttpClientOptions();
        options.setUseAlpn(true);
        options.setSsl(true);
        Buffer buffer;
        try (InputStream stream = new FileInputStream("target/certs/grpc-tls-ca.crt")) {
            buffer = Buffer.buffer(stream.readAllBytes());
        }
        options.setTrustOptions(new PemTrustOptions().addCertValue(buffer));
        client = GrpcClient.client(vertx, options);
        channel = new GrpcClientChannel(client, SocketAddress.inetSocketAddress(8444, "localhost"));
    }

    @AfterEach
    public void cleanup() {
        if (channel != null) {
            GRPCTestUtils.close(channel);
        }
        if (client != null) {
            GRPCTestUtils.close(client);
        }
    }

    @Test
    public void testHelloWorldServiceUsingBlockingStub() {
        GreeterGrpc.GreeterBlockingStub client = GreeterGrpc.newBlockingStub(channel);
        HelloReply reply = client
                .sayHello(HelloRequest.newBuilder().setName("neo-blocking").build());
        assertThat(reply.getMessage()).isEqualTo("Hello neo-blocking");
    }

    @Test
    public void testHelloWorldServiceUsingMutinyStub() {
        HelloReply reply = MutinyGreeterGrpc.newMutinyStub(channel)
                .sayHello(HelloRequest.newBuilder().setName("neo-blocking").build())
                .await().atMost(Duration.ofSeconds(5));
        assertThat(reply.getMessage()).isEqualTo("Hello neo-blocking");
    }

}
