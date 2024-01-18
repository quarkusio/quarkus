package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcService;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloServiceImpl extends MutinyGreeterGrpc.GreeterImplBase {

    @Inject
    CurrentIdentityAssociation identityAssociation;

    @RolesAllowed("admin")
    @Override
    public Uni<HelloReply> sayHelloAdmin(HelloRequest request) {
        return sayHello(request);
    }

    @RolesAllowed("tester")
    @Override
    public Uni<HelloReply> sayHelloTester(HelloRequest request) {
        return sayHello(request);
    }

    private Uni<HelloReply> sayHello(HelloRequest request) {
        String name = request.getName();
        return identityAssociation.getDeferredIdentity().map(securityIdentity -> HelloReply.newBuilder()
                .setMessage("Hello " + name + " from " + securityIdentity.getPrincipal().getName()).build());
    }
}
