package io.quarkus.security.webauthn;

import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Endpoints for login/register/callback
 */
public class WebAuthnController {

    private WebAuthnSecurity security;

    public WebAuthnController(WebAuthnSecurity security) {
        this.security = security;
    }

    /**
     * Endpoint for getting a list of allowed origins
     *
     * @param ctx the current request
     */
    public void wellKnown(RoutingContext ctx) {
        try {
            ctx.response()
                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .end(new JsonObject()
                            .put("origins", security.getAllowedOrigins(ctx))
                            .encode());
        } catch (IllegalArgumentException e) {
            ctx.fail(400, e);
        } catch (RuntimeException e) {
            ctx.fail(e);
        }
    }

    /**
     * Endpoint for getting a register challenge and options
     *
     * @param ctx the current request
     */
    public void registerOptionsChallenge(RoutingContext ctx) {
        try {
            // might throw runtime exception if there's no json or is bad formed
            final JsonObject webauthnRegister = ctx.getBodyAsJson();

            String name = webauthnRegister.getString("name");
            String displayName = webauthnRegister.getString("displayName");
            withContext(() -> security.getRegisterChallenge(name, displayName, ctx))
                    .map(challenge -> security.toJsonString(challenge))
                    .subscribe().with(challenge -> ok(ctx, challenge), ctx::fail);

        } catch (IllegalArgumentException e) {
            ctx.fail(400, e);
        } catch (RuntimeException e) {
            ctx.fail(e);
        }
    }

    private <T> Uni<T> withContext(Supplier<Uni<T>> uni) {
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        ContextState contextState = requestContext.getState();
        return uni.get().eventually(() -> requestContext.destroy(contextState));
    }

    /**
     * Endpoint for getting a login challenge and options
     *
     * @param ctx the current request
     */
    public void loginOptionsChallenge(RoutingContext ctx) {
        try {
            // might throw runtime exception if there's no json or is bad formed
            final JsonObject webauthnLogin = ctx.getBodyAsJson();

            String name = webauthnLogin.getString("name");
            withContext(() -> security.getLoginChallenge(name, ctx))
                    .map(challenge -> security.toJsonString(challenge))
                    .subscribe().with(challenge -> ok(ctx, challenge), ctx::fail);

        } catch (IllegalArgumentException e) {
            ctx.fail(400, e);
        } catch (RuntimeException e) {
            ctx.fail(e);
        }

    }

    /**
     * Endpoint for login. This will call {@link}
     *
     * @param ctx the current request
     */
    public void login(RoutingContext ctx) {
        try {
            // might throw runtime exception if there's no json or is bad formed
            final JsonObject webauthnResp = ctx.getBodyAsJson();

            withContext(() -> security.login(webauthnResp, ctx))
                    .onItem().call(record -> security.storage().update(record.getCredentialID(), record.getCounter()))
                    .subscribe().with(record -> {
                        security.rememberUser(record.getUserName(), ctx);
                        ok(ctx);
                    }, x -> ctx.fail(400, x));
        } catch (IllegalArgumentException e) {
            ctx.fail(400, e);
        } catch (RuntimeException e) {
            ctx.fail(e);
        }

    }

    /**
     * Endpoint for registration
     *
     * @param ctx the current request
     */
    public void register(RoutingContext ctx) {
        try {
            final String username = ctx.queryParams().get("username");
            // might throw runtime exception if there's no json or is bad formed
            final JsonObject webauthnResp = ctx.getBodyAsJson();

            withContext(() -> security.register(username, webauthnResp, ctx))
                    .onItem().call(record -> security.storage().create(record))
                    .subscribe().with(record -> {
                        security.rememberUser(record.getUserName(), ctx);
                        ok(ctx);
                    }, x -> ctx.fail(400, x));
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
        security.logout(ctx);
        ctx.redirect("/");
    }

    private static void ok(RoutingContext ctx, String json) {
        ctx.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(json);
    }

    private static void ok(RoutingContext ctx) {
        ctx.response()
                .setStatusCode(204)
                .end();
    }

    public void javascript(RoutingContext ctx) {
        ctx.response().sendFile("webauthn.js");
    }
}
