package io.quarkus.grpc.examples.hello;

import java.io.File;

import javax.net.ssl.SSLException;

import org.junit.jupiter.api.BeforeEach;

import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HelloWorldTlsServiceTest extends HelloWorldTlsServiceTestBase {

    @BeforeEach
    public void init() throws SSLException {
        SslContextBuilder builder = GrpcSslContexts.forClient();
        builder.trustManager(new File("target/certs/grpc-tls-ca.crt"));
        SslContext context = builder.build();

        channel = NettyChannelBuilder.forAddress("localhost", 9001)
                .sslContext(context)
                .build();
    }

}
