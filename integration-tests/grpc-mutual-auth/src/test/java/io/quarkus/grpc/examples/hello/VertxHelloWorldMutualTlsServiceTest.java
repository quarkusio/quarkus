package io.quarkus.grpc.examples.hello;

import java.io.InputStream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientChannel;

@QuarkusTest
@TestProfile(VertxGRPCTestProfile.class)
@Disabled
class VertxHelloWorldMutualTlsServiceTest extends HelloWorldMutualTlsServiceTestBase {

    @Inject
    Vertx vertx;

    GrpcClient client;

    @BeforeEach
    public void init() throws Exception {
        HttpClientOptions options = new HttpClientOptions();
        options.setUseAlpn(true);
        options.setSsl(true);
        Buffer buffer;
        try (InputStream stream = stream("tls/ca.pem")) {
            buffer = Buffer.buffer(stream.readAllBytes());
        }
        Buffer cb;
        try (InputStream stream = stream("tls/client.pem")) {
            cb = Buffer.buffer(stream.readAllBytes());
        }
        Buffer ck;
        try (InputStream stream = stream("tls/client.key")) {
            ck = Buffer.buffer(stream.readAllBytes());
        }
        options.setTrustOptions(new PemTrustOptions().addCertValue(buffer));
        options.setKeyCertOptions(new PemKeyCertOptions().setCertValue(cb).setKeyValue(ck));
        client = GrpcClient.client(vertx, options);
        channel = new GrpcClientChannel(client, SocketAddress.inetSocketAddress(8444, "localhost"));
    }

    @AfterEach
    public void cleanup() {
        client.close().toCompletionStage().toCompletableFuture().join();
    }

}
