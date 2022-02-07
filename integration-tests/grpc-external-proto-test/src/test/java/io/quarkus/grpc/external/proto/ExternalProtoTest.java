package io.quarkus.grpc.external.proto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.test.MyProto.TextContainer;
import com.test.MyTest;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;

@QuarkusTest
public class ExternalProtoTest {
    @GrpcClient
    MyTest testClient;

    @Test
    void shouldWorkWithClientAndServiceFromExternalProto() {
        Uni<TextContainer> reply = testClient.doTest(TextContainer.newBuilder().setText("my-request").build());
        String replyText = reply.await().atMost(Duration.ofSeconds(30))
                .getText();
        assertThat(replyText).isEqualTo("reply_to:my-request");
    }
}
