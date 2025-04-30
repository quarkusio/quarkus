package io.quarkus.it.elytron.oauth2;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.grpc.Metadata;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcClientUtils;

@Path("/api")
public class ElytronOauth2ExtensionResource {

    private static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of("Authorization",
            Metadata.ASCII_STRING_MARSHALLER);

    @GrpcClient("hello")
    MutinyGreeterGrpc.MutinyGreeterStub helloClient;

    @GET
    @Path("/anonymous")
    public String anonymous() {
        return "anonymous";
    }

    @GET
    @Path("/authenticated")
    @RolesAllowed("READER")
    public String authenticated() {
        return "authenticated";
    }

    @GET
    @Path("/forbidden")
    @RolesAllowed("WRITER")
    public String forbidden() {
        return "forbidden";
    }

    @PermitAll
    @GET
    @Path("/grpc-reader")
    public String grpcReader(@HeaderParam("Authorization") String authorization) {
        Metadata metadata = new Metadata();
        metadata.put(AUTHORIZATION, authorization);
        return GrpcClientUtils.attachHeaders(helloClient, metadata)
                .sayHelloReader(HelloRequest.newBuilder().setName("Ron").build()).map(HelloReply::getMessage).await()
                .indefinitely();
    }

    @PermitAll
    @GET
    @Path("/grpc-writer")
    public String grpcWriter(@HeaderParam("Authorization") String authorization) {
        Metadata metadata = new Metadata();
        metadata.put(AUTHORIZATION, authorization);
        return GrpcClientUtils.attachHeaders(helloClient, metadata)
                .sayHelloWriter(HelloRequest.newBuilder().setName("Rudolf").build()).map(HelloReply::getMessage)
                .await()
                .indefinitely();
    }

}
