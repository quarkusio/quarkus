package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.grpc.Metadata;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcClientUtils;

@Path("hello")
public class HelloResource {

    private static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of("Authorization",
            Metadata.ASCII_STRING_MARSHALLER);

    @GrpcClient("hello")
    MutinyGreeterGrpc.MutinyGreeterStub helloClient;

    @GET
    @Path("admin")
    public String helloAdmin(@HeaderParam("Authorization") String authorization) {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION, authorization);
        return GrpcClientUtils.attachHeaders(helloClient, headers)
                .sayHelloAdmin(HelloRequest.newBuilder().setName("Jonathan").build()).map(HelloReply::getMessage).await()
                .indefinitely();
    }

    @GET
    @Path("tester")
    public String helloTester(@HeaderParam("Authorization") String authorization) {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION, authorization);
        return GrpcClientUtils.attachHeaders(helloClient, headers)
                .sayHelloTester(HelloRequest.newBuilder().setName("Severus").build()).map(HelloReply::getMessage).await()
                .indefinitely();
    }

}
