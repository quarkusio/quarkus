package io.quarkus.it.elytron.oauth2;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloService extends MutinyGreeterGrpc.GreeterImplBase {

    @Inject
    SecurityIdentity identity;

    @RolesAllowed("READER")
    @Override
    public Uni<HelloReply> sayHelloReader(HelloRequest request) {
        return Uni.createFrom().item(HelloReply.newBuilder()
                .setMessage("Hello " + request.getName() + " from " + identity.getPrincipal().getName()).build());
    }

    @RolesAllowed("WRITER")
    @Override
    public Uni<HelloReply> sayHelloWriter(HelloRequest request) {
        return Uni.createFrom().item(HelloReply.newBuilder()
                .setMessage("Hello " + request.getName() + " from " + identity.getPrincipal().getName()).build());
    }
}
