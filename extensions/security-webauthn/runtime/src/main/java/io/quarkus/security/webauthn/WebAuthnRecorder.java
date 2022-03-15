package io.quarkus.security.webauthn;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.security.PersistentLoginManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

@Recorder
public class WebAuthnRecorder {

    private static final Logger log = Logger.getLogger(WebAuthnRecorder.class);

    final RuntimeValue<HttpConfiguration> httpConfiguration;
    final RuntimeValue<WebAuthnRunTimeConfig> config;

    //the temp encryption key, persistent across dev mode restarts
    static volatile String encryptionKey;

    public WebAuthnRecorder(RuntimeValue<HttpConfiguration> httpConfiguration, RuntimeValue<WebAuthnRunTimeConfig> config) {
        this.httpConfiguration = httpConfiguration;
        this.config = config;
    }

    public void setupRoutes(BeanContainer beanContainer, RuntimeValue<Router> routerValue) {
        WebAuthnSecurity security = beanContainer.instance(WebAuthnSecurity.class);
        WebAuthnAuthenticationMechanism authMech = beanContainer.instance(WebAuthnAuthenticationMechanism.class);
        IdentityProviderManager identityProviderManager = beanContainer.instance(IdentityProviderManager.class);
        WebAuthnController controller = new WebAuthnController(security, config.getValue(), identityProviderManager, authMech);
        Router router = routerValue.getValue();
        BodyHandler bodyHandler = BodyHandler.create();
        // FIXME: paths configurable
        router.post("/webauthn/login").handler(bodyHandler).handler(controller::login);
        router.post("/webauthn/register").handler(bodyHandler).handler(controller::register);
        router.post("/webauthn/callback").handler(bodyHandler).handler(controller::callback);
        router.get("/webauthn/webauthn.js").handler(controller::javascript);
        router.get("/webauthn/logout").handler(controller::logout);
    }

    public Supplier<WebAuthnAuthenticationMechanism> setupWebAuthnAuthenticationMechanism() {

        return new Supplier<WebAuthnAuthenticationMechanism>() {
            @Override
            public WebAuthnAuthenticationMechanism get() {
                String key;
                if (!httpConfiguration.getValue().encryptionKey.isPresent()) {
                    if (encryptionKey != null) {
                        //persist across dev mode restarts
                        key = encryptionKey;
                    } else {
                        byte[] data = new byte[32];
                        new SecureRandom().nextBytes(data);
                        key = encryptionKey = Base64.getEncoder().encodeToString(data);
                        log.warn("Encryption key was not specified for persistent WebAuthn auth, using temporary key " + key);
                    }
                } else {
                    key = httpConfiguration.getValue().encryptionKey.get();
                }
                WebAuthnRunTimeConfig config = WebAuthnRecorder.this.config.getValue();
                PersistentLoginManager loginManager = new PersistentLoginManager(key, config.cookieName,
                        config.sessionTimeout.toMillis(),
                        config.newCookieInterval.toMillis());
                String loginPage = config.loginPage.startsWith("/") ? config.loginPage : "/" + config.loginPage;
                return new WebAuthnAuthenticationMechanism(loginManager, loginPage);
            }
        };
    }
}
