package io.quarkus.grpc.examples.hello;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloWorldTlsService extends MutinyGreeterGrpc.GreeterImplBase {

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        String name = request.getName();
        return Uni.createFrom().item("Hello " + name)
                .map(res -> HelloReply.newBuilder().setMessage(res).build());
    }

    @RolesAllowed("admin")
    @Override
    public Uni<HelloReply> sayHelloRoleAdmin(HelloRequest request) {
        String name = request.getName();
        return Uni.createFrom().item("Hello " + name + " from " + securityIdentity.getPrincipal().getName())
                .map(res -> HelloReply.newBuilder().setMessage(res).build());
    }

    @RolesAllowed("user")
    @Override
    public Uni<HelloReply> sayHelloRoleUser(HelloRequest request) {
        String name = request.getName();
        return Uni.createFrom().item("Hello " + name + " from " + securityIdentity.getPrincipal().getName())
                .map(res -> HelloReply.newBuilder().setMessage(res).build());
    }
}
