package io.quarkus.grpc.server;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.protobuf.EmptyProtos;

import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.GreeterBean;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloReplyOrBuilder;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloRequestOrBuilder;
import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.testing.integration.Messages;
import io.grpc.testing.integration.MutinyTestServiceGrpc;
import io.grpc.testing.integration.TestServiceGrpc;
import io.netty.handler.ssl.SslContext;
import io.quarkus.grpc.server.services.AssertHelper;
import io.quarkus.grpc.server.services.MutinyHelloService;
import io.quarkus.grpc.server.services.MutinyTestService;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test services exposed by the gRPC server implemented using the regular gRPC model.
 * Communication uses TLS.
 */
public class MutinyGrpcServiceWithSSLTest extends GrpcServiceTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setFlatClassPath(true).setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(MutinyHelloService.class, MutinyTestService.class, AssertHelper.class,
                                    GreeterGrpc.class, Greeter.class, GreeterBean.class, HelloRequest.class, HelloReply.class,
                                    MutinyGreeterGrpc.class,
                                    HelloRequestOrBuilder.class, HelloReplyOrBuilder.class,
                                    EmptyProtos.class, Messages.class, MutinyTestServiceGrpc.class,
                                    TestServiceGrpc.class))
            .withConfigurationResource("grpc-server-tls-configuration.properties");

    @Override
    @BeforeEach
    public void init() throws Exception {
        SslContext sslcontext = GrpcSslContexts.forClient()
                .trustManager(createTrustAllTrustManager())
                .build();
        channel = NettyChannelBuilder.forAddress("localhost", 9001)
                .sslContext(sslcontext)
                .build();
    }

    // Create a TrustManager which trusts everything
    private static TrustManager createTrustAllTrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

}
