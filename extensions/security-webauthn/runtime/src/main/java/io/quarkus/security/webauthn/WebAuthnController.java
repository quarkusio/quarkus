package io.quarkus.security.webauthn;

import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.quarkus.vertx.http.runtime.security.PersistentLoginManager.RestoreResult;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.webauthn.WebAuthnCredentials;
import io.vertx.ext.auth.webauthn.impl.attestation.AttestationException;
import io.vertx.ext.web.RoutingContext;

/**
 * Endpoints for login/register/callback
 */
public class WebAuthnController {

    private static final Logger log = Logger.getLogger(WebAuthnController.class);

    public static final String USERNAME_COOKIE = "_quarkus_webauthn_username";

    public static final String CHALLENGE_COOKIE = "_quarkus_webauthn_challenge";

    private WebAuthnSecurity security;

    private String origin;

    private String domain;

    private IdentityProviderManager identityProviderManager;

    private WebAuthnAuthenticationMechanism authMech;

    public WebAuthnController(WebAuthnSecurity security, WebAuthnRunTimeConfig config,
            IdentityProviderManager identityProviderManager,
            WebAuthnAuthenticationMechanism authMech) {
        origin = config.origin.orElse(null);
        domain = config.domain.orElse(null);
        this.security = security;
        this.identityProviderManager = identityProviderManager;
        this.authMech = authMech;
    }

    private static boolean containsRequiredString(JsonObject json, String key) {
        try {
            if (json == null) {
                return false;
            }
            if (!json.containsKey(key)) {
                return false;
            }
            Object s = json.getValue(key);
            return (s instanceof String) && !"".equals(s);
        } catch (ClassCastException e) {
            return false;
        }
    }

    private static boolean containsOptionalString(JsonObject json, String key) {
        try {
            if (json == null) {
                return true;
            }
            if (!json.containsKey(key)) {
                return true;
            }
            Object s = json.getValue(key);
            return (s instanceof String);
        } catch (ClassCastException e) {
            return false;
        }
    }

    private static boolean containsRequiredObject(JsonObject json, String key) {
        try {
            if (json == null) {
                return false;
            }
            if (!json.containsKey(key)) {
                return false;
            }
            JsonObject s = json.getJsonObject(key);
            return s != null;
        } catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Endpoint for register
     *
     * @param ctx the current request
     */
    public void register(RoutingContext ctx) {
        try {
            // might throw runtime exception if there's no json or is bad formed
            final JsonObject webauthnRegister = ctx.getBodyAsJson();

            // the register object should match a Webauthn user.
            // A user has only a required field: name
            // And optional fields: displayName and icon
            if (webauthnRegister == null || !containsRequiredString(webauthnRegister, "name")) {
                ctx.fail(400, new IllegalArgumentException("missing 'name' field from request json"));
            } else {
                // input basic validation is OK

                ManagedContext requestContext = Arc.container().requestContext();
                requestContext.activate();
                ContextState contextState = requestContext.getState();
                security.getWebAuthn().createCredentialsOptions(webauthnRegister, createCredentialsOptions -> {
                    requestContext.destroy(contextState);
                    if (createCredentialsOptions.failed()) {
                        ctx.fail(createCredentialsOptions.cause());
                        return;
                    }

                    final JsonObject credentialsOptions = createCredentialsOptions.result();

                    // save challenge to the session
                    authMech.getLoginManager().save(credentialsOptions.getString("challenge"), ctx, CHALLENGE_COOKIE, null,
                            ctx.request().isSSL());
                    authMech.getLoginManager().save(webauthnRegister.getString("name"), ctx, USERNAME_COOKIE, null,
                            ctx.request().isSSL());

                    ok(ctx, credentialsOptions);
                });
            }
        } catch (IllegalArgumentException e) {
            ctx.fail(400, e);
        } catch (RuntimeException e) {
            ctx.fail(e);
        }
    }

    /**
     * Endpoint for login
     *
     * @param ctx the current request
     */
    public void login(RoutingContext ctx) {
        try {
            // might throw runtime exception if there's no json or is bad formed
            final JsonObject webauthnLogin = ctx.getBodyAsJson();

            if (webauthnLogin == null || !containsRequiredString(webauthnLogin, "name")) {
                ctx.fail(400, new IllegalArgumentException("Request missing 'name' field"));
                return;
            }

            // input basic validation is OK

            final String username = webauthnLogin.getString("name");

            ManagedContext requestContext = Arc.container().requestContext();
            requestContext.activate();
            ContextState contextState = requestContext.getState();
            // STEP 18 Generate assertion
            security.getWebAuthn().getCredentialsOptions(username, generateServerGetAssertion -> {
                requestContext.destroy(contextState);
                if (generateServerGetAssertion.failed()) {
                    ctx.fail(generateServerGetAssertion.cause());
                    return;
                }

                final JsonObject getAssertion = generateServerGetAssertion.result();

                authMech.getLoginManager().save(getAssertion.getString("challenge"), ctx, CHALLENGE_COOKIE, null,
                        ctx.request().isSSL());
                authMech.getLoginManager().save(username, ctx, USERNAME_COOKIE, null, ctx.request().isSSL());

                ok(ctx, getAssertion);
            });
        } catch (IllegalArgumentException e) {
            ctx.fail(400, e);
        } catch (RuntimeException e) {
            ctx.fail(e);
        }

    }

    /**
     * Endpoint for callback
     *
     * @param ctx the current request
     */
    public void callback(RoutingContext ctx) {
        try {
            // might throw runtime exception if there's no json or is bad formed
            final JsonObject webauthnResp = ctx.getBodyAsJson();
            // input validation
            if (webauthnResp == null ||
                    !containsRequiredString(webauthnResp, "id") ||
                    !containsRequiredString(webauthnResp, "rawId") ||
                    !containsRequiredObject(webauthnResp, "response") ||
                    !containsOptionalString(webauthnResp.getJsonObject("response"), "userHandle") ||
                    !containsRequiredString(webauthnResp, "type") ||
                    !"public-key".equals(webauthnResp.getString("type"))) {

                ctx.fail(400, new IllegalArgumentException(
                        "Response missing one or more of id/rawId/response[.userHandle]/type fields, or type is not public-key"));
                return;
            }

            RestoreResult challenge = authMech.getLoginManager().restore(ctx, CHALLENGE_COOKIE);
            RestoreResult username = authMech.getLoginManager().restore(ctx, USERNAME_COOKIE);
            if (challenge == null || challenge.getPrincipal() == null || challenge.getPrincipal().isEmpty()
                    || username == null || username.getPrincipal() == null || username.getPrincipal().isEmpty()) {
                ctx.fail(400, new IllegalArgumentException("Missing challenge or username"));
                return;
            }

            ManagedContext requestContext = Arc.container().requestContext();
            requestContext.activate();
            ContextState contextState = requestContext.getState();
            // input basic validation is OK
            // authInfo
            WebAuthnCredentials credentials = new WebAuthnCredentials()
                    .setOrigin(origin)
                    .setDomain(domain)
                    .setChallenge(challenge.getPrincipal())
                    .setUsername(username.getPrincipal())
                    .setWebauthn(webauthnResp);
            identityProviderManager
                    .authenticate(HttpSecurityUtils
                            .setRoutingContextAttribute(new WebAuthnAuthenticationRequest(credentials), ctx))
                    .subscribe().with(new Consumer<SecurityIdentity>() {
                        @Override
                        public void accept(SecurityIdentity identity) {
                            requestContext.destroy(contextState);
                            // invalidate the challenge
                            WebAuthnSecurity.removeCookie(ctx, WebAuthnController.CHALLENGE_COOKIE);
                            WebAuthnSecurity.removeCookie(ctx, WebAuthnController.USERNAME_COOKIE);
                            try {
                                authMech.getLoginManager().save(identity, ctx, null, ctx.request().isSSL());
                                ok(ctx);
                            } catch (Throwable t) {
                                log.error("Unable to complete post authentication", t);
                                ctx.fail(t);
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            requestContext.terminate();
                            if (throwable instanceof AttestationException) {
                                ctx.fail(400, throwable);
                            } else {
                                ctx.fail(throwable);
                            }
                        }
                    });
        } catch (IllegalArgumentException e) {
            ctx.fail(400, e);
        } catch (RuntimeException e) {
            ctx.fail(e);
        }

    }

    /**
     * Endpoint for logout, redirects to the root URI
     *
     * @param ctx the current request
     */
    public void logout(RoutingContext ctx) {
        authMech.getLoginManager().clear(ctx);
        ctx.redirect("/");
    }

    private static void ok(RoutingContext ctx) {
        ctx.response()
                .setStatusCode(204)
                .end();
    }

    private static void ok(RoutingContext ctx, JsonObject result) {
        ctx.json(result);
    }

    public void javascript(RoutingContext ctx) {
        ctx.response().sendFile("webauthn.js");
    }
}
