package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import examples.MutinySaluterGrpc;
import examples.SaluteReply;
import examples.SaluteRequest;
import io.quarkus.grpc.GrpcService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@RolesAllowed("admin")
@GrpcService
public class SaluterServiceImpl extends MutinySaluterGrpc.SaluterImplBase {

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public Uni<SaluteReply> bearer(SaluteRequest request) {
        var principalName = securityIdentity.getPrincipal().getName();
        return Uni.createFrom().item(SaluteReply.newBuilder()
                .setMessage("Hi " + request.getName() + " from " + principalName).build());
    }
}
