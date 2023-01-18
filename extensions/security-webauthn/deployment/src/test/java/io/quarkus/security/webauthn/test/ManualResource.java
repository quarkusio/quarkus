package io.quarkus.security.webauthn.test;

import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.quarkus.security.webauthn.WebAuthnLoginResponse;
import io.quarkus.security.webauthn.WebAuthnRegisterResponse;
import io.quarkus.security.webauthn.WebAuthnSecurity;
import io.quarkus.test.security.webauthn.WebAuthnTestUserProvider;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Path("/")
public class ManualResource {

    @Inject
    WebAuthnSecurity security;

    @Inject
    WebAuthnTestUserProvider userProvider;

    @Path("register")
    @POST
    public Uni<String> register(@BeanParam WebAuthnRegisterResponse register, RoutingContext ctx) {
        return security.register(register, ctx).map(authenticator -> {
            // need to attach the authenticator to the user
            userProvider.store(authenticator);
            security.rememberUser(authenticator.getUserName(), ctx);
            return "OK";
        });
    }

    @Path("login")
    @POST
    public Uni<String> register(@BeanParam WebAuthnLoginResponse login, RoutingContext ctx) {
        return security.login(login, ctx).map(authenticator -> {
            // need to update the user's authenticator
            userProvider.update(authenticator.getUserName(), authenticator.getCredID(), authenticator.getCounter());
            security.rememberUser(authenticator.getUserName(), ctx);
            return "OK";
        });
    }
}
