package io.quarkus.grpc.server;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.protobuf.EmptyProtos;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloReplyOrBuilder;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloRequestOrBuilder;
import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.grpc.testing.integration.Messages;
import io.grpc.testing.integration.MutinyTestServiceGrpc;
import io.grpc.testing.integration.TestServiceGrpc;
import io.quarkus.grpc.server.services.AssertHelper;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.grpc.server.services.TestService;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test services exposed by the gRPC server implemented using the regular gRPC model.
 * Communication uses plain-text.
 */
public class RegularGrpcServiceWithPlainTextTest extends GrpcServiceTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setFlatClassPath(true).setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(HelloService.class, TestService.class, AssertHelper.class,
                                    GreeterGrpc.class, HelloRequest.class, HelloReply.class, MutinyGreeterGrpc.class,
                                    HelloRequestOrBuilder.class, HelloReplyOrBuilder.class,
                                    EmptyProtos.class, Messages.class, MutinyTestServiceGrpc.class,
                                    TestServiceGrpc.class));

}
