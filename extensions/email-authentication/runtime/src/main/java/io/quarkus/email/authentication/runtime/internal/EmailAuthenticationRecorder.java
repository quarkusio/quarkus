package io.quarkus.email.authentication.runtime.internal;

import java.security.SecureRandom;
import java.util.Base64;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.vertx.http.runtime.security.SecurityHandlerPriorities;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class EmailAuthenticationRecorder {

    /**
     * Internal configuration property name used during the dev mode to assure that the generated encryption key
     * will not change due to the application restart, thus supporting original session cookies.
     */
    public static final String LIVE_RELOAD_ENCRYPTION_KEY = "live-reload.quarkus.email.authentication.encryption-key";

    private final RuntimeValue<EmailAuthenticationConfig> configRuntimeValue;

    EmailAuthenticationRecorder(RuntimeValue<EmailAuthenticationConfig> configRuntimeValue) {
        this.configRuntimeValue = configRuntimeValue;
    }

    /**
     * Makes sure that email authentication works with disabled eager authentication.
     * This is similar to what we do for the form-based authentication, thus refer there for more context.
     */
    public void registerEmailAuthRouteHandler(RuntimeValue<Router> httpRouter) {
        var config = configRuntimeValue.getValue();
        var handler = createEmailAuthRouteHandler();
        httpRouter.getValue()
                .post(config.codeGenerationLocation())
                .order(-1 * SecurityHandlerPriorities.FORM_AUTHENTICATION)
                .handler(handler);
        httpRouter.getValue()
                .post(config.postLocation())
                .order(-1 * SecurityHandlerPriorities.FORM_AUTHENTICATION)
                .handler(handler);
    }

    private static Handler<RoutingContext> createEmailAuthRouteHandler() {
        return new Handler<RoutingContext>() {

            @Override
            public void handle(RoutingContext event) {
                Uni<SecurityIdentity> user = event.get(QuarkusHttpUser.DEFERRED_IDENTITY_KEY);
                user.subscribe().withSubscriber(new UniSubscriber<SecurityIdentity>() {
                    @Override
                    public void onSubscribe(UniSubscription uniSubscription) {

                    }

                    @Override
                    public void onItem(SecurityIdentity securityIdentity) {
                        if (!event.response().ended()) {
                            event.response().end();
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        if (!event.response().ended() && !event.failed()) {
                            event.fail(throwable);
                        }
                    }
                });
            }
        };
    }

    public static String generateEncryptionKey() {
        byte[] data = new byte[32];
        new SecureRandom().nextBytes(data);
        return Base64.getEncoder().encodeToString(data);
    }
}
