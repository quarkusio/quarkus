package io.quarkus.it.security.webauthn;

import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.jboss.resteasy.reactive.RestForm;

import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.quarkus.security.webauthn.WebAuthnLoginResponse;
import io.quarkus.security.webauthn.WebAuthnRegisterResponse;
import io.quarkus.security.webauthn.WebAuthnSecurity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.auth.webauthn.Authenticator;
import io.vertx.ext.web.RoutingContext;

@Path("")
public class LoginResource {

    @Inject
    WebAuthnSecurity webAuthnSecurity;

    @Path("/login")
    @POST
    @ReactiveTransactional
    public Uni<Response> login(@RestForm String userName,
            @BeanParam WebAuthnLoginResponse webAuthnResponse,
            RoutingContext ctx) {
        // Input validation
        if (userName == null || userName.isEmpty()
                || !webAuthnResponse.isSet()
                || !webAuthnResponse.isValid()) {
            return Uni.createFrom().item(Response.status(Status.BAD_REQUEST).build());
        }

        Uni<User> userUni = User.findByUserName(userName);
        return userUni.flatMap(user -> {
            if (user == null) {
                // Invalid user
                return Uni.createFrom().item(Response.status(Status.BAD_REQUEST).build());
            }
            Uni<Authenticator> authenticator = this.webAuthnSecurity.login(webAuthnResponse, ctx);

            return authenticator
                    // bump the auth counter
                    .invoke(auth -> user.webAuthnCredential.counter = auth.getCounter())
                    .map(auth -> {
                        // make a login cookie
                        this.webAuthnSecurity.rememberUser(auth.getUserName(), ctx);
                        return Response.ok().build();
                    })
                    // handle login failure
                    .onFailure().recoverWithItem(x -> {
                        // make a proper error response
                        return Response.status(Status.BAD_REQUEST).build();
                    });

        });
    }

    @Path("/register")
    @POST
    @ReactiveTransactional
    public Uni<Response> register(@RestForm String userName,
            @BeanParam WebAuthnRegisterResponse webAuthnResponse,
            RoutingContext ctx) {
        // Input validation
        if (userName == null || userName.isEmpty()
                || !webAuthnResponse.isSet()
                || !webAuthnResponse.isValid()) {
            return Uni.createFrom().item(Response.status(Status.BAD_REQUEST).build());
        }

        Uni<User> userUni = User.findByUserName(userName);
        return userUni.flatMap(user -> {
            if (user != null) {
                // Duplicate user
                return Uni.createFrom().item(Response.status(Status.BAD_REQUEST).build());
            }
            Uni<Authenticator> authenticator = this.webAuthnSecurity.register(webAuthnResponse, ctx);

            return authenticator
                    // store the user
                    .flatMap(auth -> {
                        User newUser = new User();
                        newUser.userName = auth.getUserName();
                        WebAuthnCredential credential = new WebAuthnCredential(auth, newUser);
                        return credential.persist()
                                .flatMap(c -> newUser.<User> persist());

                    })
                    .map(newUser -> {
                        // make a login cookie
                        this.webAuthnSecurity.rememberUser(newUser.userName, ctx);
                        return Response.ok().build();
                    })
                    // handle login failure
                    .onFailure().recoverWithItem(x -> {
                        // make a proper error response
                        return Response.status(Status.BAD_REQUEST).build();
                    });

        });
    }
}
