package io.quarkus.grpc.examples.hello;

import static io.quarkus.grpc.test.utils.GRPCTestUtils.stream;

import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.grpc.ManagedChannel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HelloWorldMutualTlsServiceTest extends HelloWorldMutualTlsServiceTestBase {

    @BeforeEach
    public void init() throws Exception {
        SslContextBuilder builder = GrpcSslContexts.forClient();
        try (InputStream cais = stream("tls/ca.pem")) {
            builder.trustManager(cais);
        }
        try (InputStream ccis = stream("tls/client.pem")) {
            try (InputStream ckis = stream("tls/client.key")) {
                builder.keyManager(ccis, ckis);
            }
        }
        SslContext context = builder.build();

        channel = NettyChannelBuilder.forAddress("localhost", 9001)
                .sslContext(context)
                .build();
    }

    @AfterEach
    public void cleanup() {
        ((ManagedChannel) channel).shutdownNow();
    }

}
