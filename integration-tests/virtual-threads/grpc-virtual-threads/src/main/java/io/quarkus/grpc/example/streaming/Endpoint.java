package io.quarkus.grpc.example.streaming;

import java.time.Duration;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import com.google.protobuf.ByteString;

import io.grpc.testing.integration.Messages;
import io.grpc.testing.integration.TestService;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.common.annotation.RunOnVirtualThread;

@Path("/endpoint")
public class Endpoint {

    @GrpcClient("service")
    TestService service;

    @GET
    @RunOnVirtualThread
    public String invokeGrpcService() {
        var req = Messages.SimpleRequest.newBuilder()
                .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8("hello")).build())
                .build();
        return service.unaryCall(req).await().atMost(Duration.ofSeconds(5))
                .getPayload().getBody().toStringUtf8();
    }
}
