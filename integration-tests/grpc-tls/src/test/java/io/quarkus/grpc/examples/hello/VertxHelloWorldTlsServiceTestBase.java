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
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientChannel;

abstract class VertxHelloWorldTlsServiceTestBase extends HelloWorldTlsServiceTestBase {

    abstract Vertx vertx();

    protected void close(Vertx vertx) {
    }

    private Vertx _vertx;
    private GrpcClient client;

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
        client = GrpcClient.client(_vertx, options);
        channel = new GrpcClientChannel(client, SocketAddress.inetSocketAddress(8444, "localhost"));
    }

    @Override
    protected void doCleanup() {
        if (client != null) {
            GRPCTestUtils.close(client);
        }
        close(_vertx);
    }
}
