package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.grpc.Metadata;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcClientUtils;
import io.smallrye.mutiny.Uni;

@Path("/api/greeter")
public class GreeterResource {

    private static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of("Authorization",
            Metadata.ASCII_STRING_MARSHALLER);

    @Inject
    JsonWebToken accessToken;

    @GrpcClient("hello")
    MutinyGreeterGrpc.MutinyGreeterStub helloClient;

    @Path("bearer")
    @GET
    public Uni<String> sayHello() {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION, "Bearer " + accessToken.getRawToken());
        return GrpcClientUtils.attachHeaders(helloClient, headers)
                .bearer(HelloRequest.newBuilder().setName("Jonathan").build()).map(HelloReply::getMessage);
    }

}
