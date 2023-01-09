package io.quarkus.security.webauthn;

import java.security.Principal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.PersistentLoginManager.RestoreResult;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.ext.auth.webauthn.Authenticator;
import io.vertx.ext.auth.webauthn.RelyingParty;
import io.vertx.ext.auth.webauthn.WebAuthn;
import io.vertx.ext.auth.webauthn.WebAuthnCredentials;
import io.vertx.ext.auth.webauthn.WebAuthnOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.Origin;

/**
 * Utility class that allows users to manually login or register users using WebAuthn
 */
@ApplicationScoped
public class WebAuthnSecurity {

    private WebAuthn webAuthn;
    private String origin;
    private String domain;

    @Inject
    WebAuthnAuthenticationMechanism authMech;

    public WebAuthnSecurity(WebAuthnRunTimeConfig config, Vertx vertx, WebAuthnAuthenticatorStorage database) {
        // create the webauthn security object
        WebAuthnOptions options = new WebAuthnOptions();
        RelyingParty relyingParty = new RelyingParty();
        if (config.relyingParty.id.isPresent()) {
            relyingParty.setId(config.relyingParty.id.get());
        }
        // this is required
        relyingParty.setName(config.relyingParty.name);
        options.setRelyingParty(relyingParty);
        if (config.attestation.isPresent()) {
            options.setAttestation(config.attestation.get());
        }
        if (config.authenticatorAttachment.isPresent()) {
            options.setAuthenticatorAttachment(config.authenticatorAttachment.get());
        }
        if (config.challengeLength.isPresent()) {
            options.setChallengeLength(config.challengeLength.getAsInt());
        }
        if (config.pubKeyCredParams.isPresent()) {
            options.setPubKeyCredParams(config.pubKeyCredParams.get());
        }
        if (config.requireResidentKey.isPresent()) {
            options.setRequireResidentKey(config.requireResidentKey.get());
        }
        if (config.timeout.isPresent()) {
            options.setTimeoutInMilliseconds(config.timeout.get().toMillis());
        }
        if (config.transports.isPresent()) {
            options.setTransports(config.transports.get());
        }
        if (config.userVerification.isPresent()) {
            options.setUserVerification(config.userVerification.get());
        }
        webAuthn = WebAuthn.create(vertx, options)
                // where to load/update authenticators data
                .authenticatorFetcher(database::fetcher)
                .authenticatorUpdater(database::updater);
        origin = config.origin.orElse(null);
        if (origin != null) {
            Origin o = Origin.parse(origin);
            domain = o.host();
        }
    }

    /**
     * Registers a new WebAuthn credentials
     *
     * @param response the Webauthn registration info
     * @param ctx the current request
     * @return the newly created credentials
     */
    public Uni<Authenticator> register(WebAuthnRegisterResponse response, RoutingContext ctx) {
        // validation of the response is done before
        RestoreResult challenge = authMech.getLoginManager().restore(ctx, WebAuthnController.CHALLENGE_COOKIE);
        RestoreResult username = authMech.getLoginManager().restore(ctx, WebAuthnController.USERNAME_COOKIE);
        if (challenge == null || challenge.getPrincipal() == null || challenge.getPrincipal().isEmpty()
                || username == null || username.getPrincipal() == null || username.getPrincipal().isEmpty()) {
            return Uni.createFrom().failure(new RuntimeException("Missing challenge or username"));
        }

        return Uni.createFrom().emitter(emitter -> {
            webAuthn.authenticate(
                    // authInfo
                    new WebAuthnCredentials()
                            .setOrigin(origin)
                            .setDomain(domain)
                            .setChallenge(challenge.getPrincipal())
                            .setUsername(username.getPrincipal())
                            .setWebauthn(response.toJsonObject()),
                    authenticate -> {
                        removeCookie(ctx, WebAuthnController.CHALLENGE_COOKIE);
                        removeCookie(ctx, WebAuthnController.USERNAME_COOKIE);
                        if (authenticate.succeeded()) {
                            // this is registration, so the caller will want to store the created Authenticator,
                            // let's recreate it
                            emitter.complete(new Authenticator(authenticate.result().principal()));
                        } else {
                            emitter.fail(authenticate.cause());
                        }
                    });
        });
    }

    /**
     * Logs an existing WebAuthn user in
     *
     * @param response the Webauthn login info
     * @param ctx the current request
     * @return the updated credentials
     */
    public Uni<Authenticator> login(WebAuthnLoginResponse response, RoutingContext ctx) {
        // validation of the response is done before
        RestoreResult challenge = authMech.getLoginManager().restore(ctx, WebAuthnController.CHALLENGE_COOKIE);
        RestoreResult username = authMech.getLoginManager().restore(ctx, WebAuthnController.USERNAME_COOKIE);
        if (challenge == null || challenge.getPrincipal() == null || challenge.getPrincipal().isEmpty()
                || username == null || username.getPrincipal() == null || username.getPrincipal().isEmpty()) {
            return Uni.createFrom().failure(new RuntimeException("Missing challenge or username"));
        }

        return Uni.createFrom().emitter(emitter -> {
            webAuthn.authenticate(
                    // authInfo
                    new WebAuthnCredentials()
                            .setOrigin(origin)
                            .setDomain(domain)
                            .setChallenge(challenge.getPrincipal())
                            .setUsername(username.getPrincipal())
                            .setWebauthn(response.toJsonObject()),
                    authenticate -> {
                        removeCookie(ctx, WebAuthnController.CHALLENGE_COOKIE);
                        removeCookie(ctx, WebAuthnController.USERNAME_COOKIE);
                        if (authenticate.succeeded()) {
                            // this is login, so the user will want to bump the counter
                            // FIXME: do we need the auth here? likely the user will know it and will just ++ on the DB-stored counter, no?
                            emitter.complete(new Authenticator(authenticate.result().principal()));
                        } else {
                            emitter.fail(authenticate.cause());
                        }
                    });
        });
    }

    static void removeCookie(RoutingContext ctx, String name) {
        // Vert.x sends back a set-cookie with max-age and expiry but no path, so we have to set it first,
        // otherwise web clients don't clear it
        Cookie cookie = ctx.request().getCookie(name);
        if (cookie != null) {
            cookie.setPath("/");
        }
        ctx.response().removeCookie(name);
    }

    /**
     * Returns the underlying Vert.x WebAuthn authenticator
     *
     * @return the underlying Vert.x WebAuthn authenticator
     */
    public WebAuthn getWebAuthn() {
        return webAuthn;
    }

    /**
     * Adds a login cookie to the current request for the given user ID
     *
     * @param userID the user ID to use as {@link Principal}
     * @param ctx the current request, in order to add a cookie
     */
    public void rememberUser(String userID, RoutingContext ctx) {
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.setPrincipal(new QuarkusPrincipal(userID));
        authMech.getLoginManager().save(builder.build(), ctx, null, ctx.request().isSSL());
    }

    /**
     * Clears the login cookie on the current request
     *
     * @param ctx the current request, in order to clear the login cookie
     */
    public void logout(RoutingContext ctx) {
        authMech.getLoginManager().clear(ctx);
    }
}
