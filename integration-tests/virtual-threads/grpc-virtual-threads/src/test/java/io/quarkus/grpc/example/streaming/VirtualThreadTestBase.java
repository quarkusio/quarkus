package io.quarkus.grpc.example.streaming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.google.protobuf.ByteString;
import com.google.protobuf.EmptyProtos;

import io.grpc.testing.integration.Messages;
import io.grpc.testing.integration.TestServiceGrpc;
import io.quarkus.grpc.GrpcClient;
import io.restassured.RestAssured;

@SuppressWarnings("NewClassNamingConvention")
public class VirtualThreadTestBase {
    @GrpcClient
    TestServiceGrpc.TestServiceBlockingStub service;

    @Test
    void testEmpty() {
        assertThat(service.emptyCall(EmptyProtos.Empty.newBuilder().build())).isEqualTo(EmptyProtos.Empty.newBuilder().build());
    }

    @Test
    void testUnary() {
        var req = Messages.SimpleRequest.newBuilder()
                .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8("hello")).build())
                .build();
        assertThat(service.unaryCall(req).getPayload().getBody().toStringUtf8()).isEqualTo("HELLO");
    }

    @Test
    void testStreamingOutputCall() {
        var req = Messages.StreamingOutputCallRequest.newBuilder()
                .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8("hello")).build())
                .build();

        AtomicInteger count = new AtomicInteger();
        service.streamingOutputCall(req).forEachRemaining(r -> {
            count.incrementAndGet();
            assertThat(r.getPayload().getBody().toStringUtf8()).isEqualTo("HELLO");
        });
        assertThat(count).hasValue(3);
    }

    @Test
    void testGrpcClient() {
        RestAssured.get("/endpoint")
                .then()
                .statusCode(200)
                .body(is("HELLO"));
    }

}
