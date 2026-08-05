package io.quarkus.grpc.external.proto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.test.MyProto.TextContainer;
import com.test.MyTest;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.external.bareproto.BareProtoMessage;
import io.quarkus.grpc.external.bareproto.BareProtoService;
import io.quarkus.grpc.external.nested.NestedMessage;
import io.quarkus.grpc.external.nested.NestedService;
import io.quarkus.grpc.external.testscope.TestScopeMessage;
import io.quarkus.grpc.external.testscope.TestScopeService;
import io.smallrye.mutiny.Uni;

public class ExternalProtoTestBase {
    @GrpcClient
    MyTest hello;

    @GrpcClient
    NestedService nested;

    @GrpcClient
    TestScopeService testScope;

    @GrpcClient
    BareProtoService bareProto;

    @Test
    void shouldWorkWithClientAndServiceFromExternalProto() {
        Uni<TextContainer> reply = hello.doTest(TextContainer.newBuilder().setText("my-request").build());
        String replyText = reply.await().atMost(Duration.ofSeconds(30))
                .getText();
        assertThat(replyText).isEqualTo("reply_to:my-request");
    }

    @Test
    void shouldWorkWithNestedProtoFromDependencyUnderSrcMainProto() {
        Uni<NestedMessage> reply = nested.send(NestedMessage.newBuilder().setContent("main").build());
        String content = reply.await().atMost(Duration.ofSeconds(30))
                .getContent();
        assertThat(content).isEqualTo("reply:main");
    }

    @Test
    void shouldWorkWithNestedProtoFromDependencyUnderSrcTestProto() {
        Uni<TestScopeMessage> reply = testScope.send(TestScopeMessage.newBuilder().setContent("test").build());
        String content = reply.await().atMost(Duration.ofSeconds(30))
                .getContent();
        assertThat(content).isEqualTo("reply:test");
    }

    @Test
    void shouldWorkWithNestedProtoFromDependencyUnderProto() {
        Uni<BareProtoMessage> reply = bareProto.send(BareProtoMessage.newBuilder().setContent("bare").build());
        String content = reply.await().atMost(Duration.ofSeconds(30))
                .getContent();
        assertThat(content).isEqualTo("reply:bare");
    }
}
