package io.quarkus.security.webauthn;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.security.PersistentLoginManager;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

@Recorder
public class WebAuthnRecorder {

    private static final Logger log = Logger.getLogger(WebAuthnRecorder.class);

    final RuntimeValue<VertxHttpConfig> httpConfig;
    final RuntimeValue<WebAuthnRunTimeConfig> config;

    //the temp encryption key, persistent across dev mode restarts
    static volatile String encryptionKey;

    public WebAuthnRecorder(RuntimeValue<VertxHttpConfig> httpConfig, RuntimeValue<WebAuthnRunTimeConfig> config) {
        this.httpConfig = httpConfig;
        this.config = config;
    }

    public RuntimeValue<WebAuthnController> controller(BeanContainer beanContainer) {
        WebAuthnSecurity security = beanContainer.beanInstance(WebAuthnSecurity.class);
        return new RuntimeValue<>(new WebAuthnController(security));
    }

    public Consumer<Route> routeFunction(String method, boolean bodyHandler) {
        return routeFunction(method, bodyHandler, true);
    }

    public Consumer<Route> loginRouteFunction() {
        return routeFunction("POST", true, config.getValue().enableLoginEndpoint().orElse(false));
    }

    public Consumer<Route> registerRouteFunction() {
        return routeFunction("POST", true, config.getValue().enableRegistrationEndpoint().orElse(false));
    }

    private static Consumer<Route> routeFunction(String method, boolean bodyHandler, boolean enabled) {
        return new Consumer<Route>() {
            @Override
            public void accept(Route route) {
                if (!enabled) {
                    route.disable();
                }
                route.method(HttpMethod.valueOf(method));
                if (bodyHandler) {
                    route.handler(BodyHandler.create());
                }
            }
        };
    }

    public Handler<RoutingContext> loginOptionsChallengeHandler(RuntimeValue<WebAuthnController> controller) {
        return controller.getValue()::loginOptionsChallenge;
    }

    public Handler<RoutingContext> registerOptionsChallengeHandler(RuntimeValue<WebAuthnController> controller) {
        return controller.getValue()::registerOptionsChallenge;
    }

    public Handler<RoutingContext> loginHandler(RuntimeValue<WebAuthnController> controller) {
        return controller.getValue()::login;
    }

    public Handler<RoutingContext> registerHandler(RuntimeValue<WebAuthnController> controller) {
        return controller.getValue()::register;
    }

    public Handler<RoutingContext> javascriptHandler(RuntimeValue<WebAuthnController> controller) {
        return controller.getValue()::javascript;
    }

    public Handler<RoutingContext> logoutHandler(RuntimeValue<WebAuthnController> controller) {
        return controller.getValue()::logout;
    }

    public Handler<RoutingContext> wellKnownHandler(RuntimeValue<WebAuthnController> controller) {
        return controller.getValue()::wellKnown;
    }

    public Supplier<WebAuthnAuthenticationMechanism> setupWebAuthnAuthenticationMechanism() {

        return new Supplier<WebAuthnAuthenticationMechanism>() {
            @Override
            public WebAuthnAuthenticationMechanism get() {
                String key;
                if (!httpConfig.getValue().encryptionKey().isPresent()) {
                    if (encryptionKey != null) {
                        //persist across dev mode restarts
                        key = encryptionKey;
                    } else {
                        byte[] data = new byte[32];
                        new SecureRandom().nextBytes(data);
                        key = encryptionKey = Base64.getEncoder().encodeToString(data);
                        log.warn(
                                "Encryption key was not specified (using `quarkus.http.auth.session.encryption-key` configuration) for persistent WebAuthn auth, using temporary key "
                                        + key);
                    }
                } else {
                    key = httpConfig.getValue().encryptionKey().get();
                }
                WebAuthnRunTimeConfig config = WebAuthnRecorder.this.config.getValue();
                PersistentLoginManager loginManager = new PersistentLoginManager(key, config.cookieName(),
                        config.sessionTimeout().toMillis(),
                        config.newCookieInterval().toMillis(), false, config.cookieSameSite().name(),
                        config.cookiePath().orElse(null),
                        config.cookieMaxAge().map(Duration::toSeconds).orElse(-1L), null);
                String loginPage = config.loginPage().startsWith("/") ? config.loginPage() : "/" + config.loginPage();
                int priority = config.priority();
                return new WebAuthnAuthenticationMechanism(loginManager, loginPage, priority);
            }
        };
    }
}
