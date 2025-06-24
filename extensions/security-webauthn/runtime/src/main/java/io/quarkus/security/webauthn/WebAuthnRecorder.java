package io.quarkus.security.webauthn;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.security.PersistentLoginManager;
import io.vertx.ext.web.Router;
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

    public void setupRoutes(BeanContainer beanContainer, RuntimeValue<Router> routerValue, String prefix) {
        WebAuthnSecurity security = beanContainer.beanInstance(WebAuthnSecurity.class);
        WebAuthnController controller = new WebAuthnController(security);
        Router router = routerValue.getValue();
        BodyHandler bodyHandler = BodyHandler.create();
        // FIXME: paths configurable
        // prefix is the non-application root path, ends with a slash: defaults to /q/
        router.get(prefix + "webauthn/login-options-challenge").handler(bodyHandler)
                .handler(controller::loginOptionsChallenge);
        router.get(prefix + "webauthn/register-options-challenge").handler(bodyHandler)
                .handler(controller::registerOptionsChallenge);
        if (config.getValue().enableLoginEndpoint().orElse(false)) {
            router.post(prefix + "webauthn/login").handler(bodyHandler).handler(controller::login);
        }
        if (config.getValue().enableRegistrationEndpoint().orElse(false)) {
            router.post(prefix + "webauthn/register").handler(bodyHandler).handler(controller::register);
        }
        router.get(prefix + "webauthn/webauthn.js").handler(controller::javascript);
        router.get(prefix + "webauthn/logout").handler(controller::logout);
        router.get("/.well-known/webauthn").handler(controller::wellKnown);
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
                return new WebAuthnAuthenticationMechanism(loginManager, loginPage);
            }
        };
    }
}
