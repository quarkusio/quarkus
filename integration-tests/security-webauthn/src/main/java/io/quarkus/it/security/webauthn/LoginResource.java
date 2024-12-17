package io.quarkus.it.security.webauthn;

import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.jboss.resteasy.reactive.RestForm;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnLoginResponse;
import io.quarkus.security.webauthn.WebAuthnRegisterResponse;
import io.quarkus.security.webauthn.WebAuthnSecurity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Path("")
public class LoginResource {

    @Inject
    WebAuthnSecurity webAuthnSecurity;

    @Path("/login")
    @POST
    @WithTransaction
    public Uni<Response> login(@RestForm String username,
            @BeanParam WebAuthnLoginResponse webAuthnResponse,
            RoutingContext ctx) {
        // Input validation
        if (username == null || username.isEmpty()
                || !webAuthnResponse.isSet()
                || !webAuthnResponse.isValid()) {
            return Uni.createFrom().item(Response.status(Status.BAD_REQUEST).build());
        }

        Uni<User> userUni = User.findByUsername(username);
        return userUni.flatMap(user -> {
            if (user == null) {
                // Invalid user
                return Uni.createFrom().item(Response.status(Status.BAD_REQUEST).build());
            }
            Uni<WebAuthnCredentialRecord> authenticator = this.webAuthnSecurity.login(webAuthnResponse, ctx);

            return authenticator
                    // bump the auth counter
                    .invoke(auth -> user.webAuthnCredential.counter = auth.getCounter())
                    .map(auth -> {
                        // make a login cookie
                        this.webAuthnSecurity.rememberUser(auth.getUsername(), ctx);
                        return Response.ok().build();
                    })
                    // handle login failure
                    .onFailure().recoverWithItem(x -> {
                        x.printStackTrace();
                        // make a proper error response
                        return Response.status(Status.BAD_REQUEST).build();
                    });

        });
    }

    @Path("/register")
    @POST
    @WithTransaction
    public Uni<Response> register(@RestForm String username,
            @BeanParam WebAuthnRegisterResponse webAuthnResponse,
            RoutingContext ctx) {
        // Input validation
        if (username == null || username.isEmpty()
                || !webAuthnResponse.isSet()
                || !webAuthnResponse.isValid()) {
            return Uni.createFrom().item(Response.status(Status.BAD_REQUEST).build());
        }

        Uni<User> userUni = User.findByUsername(username);
        return userUni.flatMap(user -> {
            if (user != null) {
                // Duplicate user
                return Uni.createFrom().item(Response.status(Status.BAD_REQUEST).build());
            }
            Uni<WebAuthnCredentialRecord> authenticator = this.webAuthnSecurity.register(username, webAuthnResponse, ctx);

            return authenticator
                    // store the user
                    .flatMap(auth -> {
                        User newUser = new User();
                        newUser.username = auth.getUsername();
                        WebAuthnCredential credential = new WebAuthnCredential(auth, newUser);
                        return credential.persist()
                                .flatMap(c -> newUser.<User> persist());

                    })
                    .map(newUser -> {
                        // make a login cookie
                        this.webAuthnSecurity.rememberUser(newUser.username, ctx);
                        return Response.ok().build();
                    })
                    // handle login failure
                    .onFailure().recoverWithItem(x -> {
                        // make a proper error response
                        x.printStackTrace();
                        return Response.status(Status.BAD_REQUEST).build();
                    });

        });
    }
}
