package io.quarkus.grpc.examples.hello;

import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;

import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpcio.client.GrpcIoClient;
import io.vertx.grpcio.client.GrpcIoClientChannel;

abstract class VertxHelloWorldTlsServiceTestBase extends HelloWorldTlsServiceTestBase {

    abstract Vertx vertx();

    protected void close(Vertx vertx) {
    }

    private Vertx _vertx;
    private GrpcIoClient client;

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
        _vertx = vertx();
        client = GrpcIoClient.client(_vertx, options);
        channel = new GrpcIoClientChannel(client, SocketAddress.inetSocketAddress(8444, "localhost"));
    }

    @Override
    protected void doCleanup() {
        if (client != null) {
            GRPCTestUtils.close(client);
        }
        close(_vertx);
    }
}
