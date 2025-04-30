package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@GrpcService
public class GreeterServiceImpl extends MutinyGreeterGrpc.GreeterImplBase {

    @Inject
    SecurityIdentity securityIdentity;

    @RolesAllowed("admin")
    @Override
    public Uni<HelloReply> bearer(HelloRequest request) {
        return Uni.createFrom().item(HelloReply.newBuilder()
                .setMessage("Hello " + request.getName() + " from " + securityIdentity.getPrincipal().getName()).build());
    }
}
