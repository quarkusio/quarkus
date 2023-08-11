package io.quarkus.oidc.deployment.devservices.keycloak;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.oidc.deployment.OidcBuildStep.IsEnabled;
import io.quarkus.oidc.deployment.OidcBuildTimeConfig;
import io.quarkus.oidc.deployment.devservices.OidcDevServicesBuildItem;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.jwt.build.Jwt;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.handler.BodyHandler;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = { IsEnabled.class, GlobalDevServicesConfig.Enabled.class })
public class LightweightDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(LightweightDevServicesProcessor.class);

    private static final String CONFIG_PREFIX = "quarkus.oidc.";
    private static final String TENANT_ENABLED_CONFIG_KEY = CONFIG_PREFIX + "tenant-enabled";
    private static final String AUTH_SERVER_URL_CONFIG_KEY = CONFIG_PREFIX + "auth-server-url";
    private static final String PROVIDER_CONFIG_KEY = CONFIG_PREFIX + "provider";
    private static final String APPLICATION_TYPE_CONFIG_KEY = CONFIG_PREFIX + "application-type";
    private static final String CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";
    private static final String CLIENT_SECRET_CONFIG_KEY = CONFIG_PREFIX + "credentials.secret";

    private static volatile RunningDevService devService;
    static volatile DevServicesConfig capturedDevServicesConfiguration;
    private static volatile boolean first = true;

    OidcBuildTimeConfig oidcConfig;

    private static volatile KeyPair kp;
    private static volatile String baseURI;
    private static volatile String clientId;

    @BuildStep
    public DevServicesResultBuildItem startLightweightServer(
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<OidcDevServicesBuildItem> oidcProviderBuildItem,
            KeycloakBuildTimeConfig config,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LaunchModeBuildItem launchMode,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            BuildProducer<LightweightDevServicesConfigBuildItem> lightweightBuildItemBuildProducer,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig devServicesConfig) {

        if (oidcProviderBuildItem.isPresent()) {
            // Dev Services for the alternative OIDC provider are enabled
            return null;
        }

        if (!config.devservices.lightweight) {
            return null;
        }
        LOG.info("Starting Lightweight OIDC dev services");

        DevServicesConfig currentDevServicesConfiguration = config.devservices;
        // Figure out if we need to shut down and restart any existing Keycloak container
        // if not and the Keycloak container has already started we just return
        if (devService != null) {
            try {
                devService.close();
            } catch (Throwable e) {
                LOG.error("Failed to stop lightweight container", e);
            }
            devService = null;
            capturedDevServicesConfiguration = null;
        }
        capturedDevServicesConfiguration = currentDevServicesConfiguration;
        try {
            List<String> errors = new ArrayList<>();

            RunningDevService newDevService = startLightweightServer(lightweightBuildItemBuildProducer,
                    !devServicesSharedNetworkBuildItem.isEmpty(),
                    devServicesConfig.timeout,
                    errors);
            if (newDevService == null) {
                return null;
            }

            devService = newDevService;

            if (first) {
                first = false;
                Runnable closeTask = new Runnable() {
                    @Override
                    public void run() {
                        if (devService != null) {
                            try {
                                devService.close();
                            } catch (Throwable t) {
                                LOG.error("Failed to stop Keycloak container", t);
                            }
                        }
                        first = true;
                        devService = null;
                        capturedDevServicesConfiguration = null;
                    }
                };
                closeBuildItem.addCloseTask(closeTask, true);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        LOG.infof("Dev Services for lightweight OIDC started on %s", baseURI);

        return devService.toBuildItem();
    }

    private RunningDevService startLightweightServer(
            BuildProducer<LightweightDevServicesConfigBuildItem> lightweightBuildItemBuildProducer,
            boolean useSharedNetwork, Optional<Duration> timeout,
            List<String> errors) {
        if (!capturedDevServicesConfiguration.enabled) {
            // explicitly disabled
            LOG.debug("Not starting Dev Services for Keycloak as it has been disabled in the config");
            return null;
        }
        if (!isOidcTenantEnabled()) {
            LOG.debug("Not starting Dev Services for Keycloak as 'quarkus.oidc.tenant.enabled' is false");
            return null;
        }
        if (ConfigUtils.isPropertyPresent(AUTH_SERVER_URL_CONFIG_KEY)) {
            LOG.debug("Not starting Dev Services for Keycloak as 'quarkus.oidc.auth-server-url' has been provided");
            return null;
        }
        if (ConfigUtils.isPropertyPresent(PROVIDER_CONFIG_KEY)) {
            LOG.debug("Not starting Dev Services for Keycloak as 'quarkus.oidc.provider' has been provided");
            return null;
        }

        Vertx vertx = Vertx.vertx();
        HttpServerOptions options = new HttpServerOptions();
        options.setPort(0);
        HttpServer httpServer = vertx.createHttpServer(options);

        Router router = Router.router(vertx);
        httpServer.requestHandler(router);
        registerRoutes(router);

        httpServer.listenAndAwait();
        int port = httpServer.actualPort();

        Map<String, String> configProperties = new HashMap<>();
        baseURI = "http://localhost:" + port;
        clientId = getOidcClientId();
        String oidcClientSecret = getOidcClientSecret();
        String oidcApplicationType = getOidcApplicationType();
        configProperties.put(AUTH_SERVER_URL_CONFIG_KEY, baseURI);
        configProperties.put(APPLICATION_TYPE_CONFIG_KEY, oidcApplicationType);
        configProperties.put(CLIENT_ID_CONFIG_KEY, clientId);
        configProperties.put(CLIENT_SECRET_CONFIG_KEY, oidcClientSecret);

        lightweightBuildItemBuildProducer
                .produce(new LightweightDevServicesConfigBuildItem(configProperties));

        return new RunningDevService("oidc-lightweight", null, () -> {
            LOG.info("Closing Vertx DEV service for oidc lightweight");
            vertx.closeAndAwait();
        }, configProperties);
    }

    private void registerRoutes(Router router) {
        BodyHandler bodyHandler = BodyHandler.create();
        router.get("/").handler(this::mainRoute);
        router.get("/.well-known/openid-configuration").handler(this::configuration);
        router.get("/authorize").handler(this::authorize);
        router.post("/login").handler(bodyHandler).handler(this::login);
        router.post("/token").handler(bodyHandler).handler(this::accessTokenJson);
        router.get("/keys").handler(this::getKeys);
        router.get("/logout").handler(this::logout);

        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        kpg.initialize(2048);
        kp = kpg.generateKeyPair();
    }

    private List<String> getUsers() {
        if (capturedDevServicesConfiguration.roles.isEmpty()) {
            return Arrays.asList("alice", "bob");
        } else {
            List<String> ret = new ArrayList<>(capturedDevServicesConfiguration.roles.keySet());
            Collections.sort(ret);
            return ret;
        }
    }

    private List<String> getUserRoles(String user) {
        List<String> roles = capturedDevServicesConfiguration.roles.get(user);
        return roles == null ? ("alice".equals(user) ? List.of("admin", "user") : List.of("user"))
                : roles;
    }

    private static boolean isOidcTenantEnabled() {
        return ConfigProvider.getConfig().getOptionalValue(TENANT_ENABLED_CONFIG_KEY, Boolean.class).orElse(true);
    }

    private static String getOidcApplicationType() {
        return ConfigProvider.getConfig().getOptionalValue(APPLICATION_TYPE_CONFIG_KEY, String.class).orElse("service");
    }

    private static String getOidcClientId() {
        return ConfigProvider.getConfig().getOptionalValue(CLIENT_ID_CONFIG_KEY, String.class)
                .orElse("quarkus-app");
    }

    private static String getOidcClientSecret() {
        return ConfigProvider.getConfig().getOptionalValue(CLIENT_SECRET_CONFIG_KEY, String.class)
                .orElse("the secret must be 32 characters at least to avoid a warning");
    }

    private void mainRoute(RoutingContext rc) {
        rc.response().endAndForget("Lightweight OIDC server up and running");
    }

    private void configuration(RoutingContext rc) {
        String data = "{\n"
                + "   \"token_endpoint\":\"" + baseURI + "/token\",\n"
                + "   \"token_endpoint_auth_methods_supported\":[\n"
                + "      \"client_secret_post\",\n"
                + "      \"private_key_jwt\",\n"
                + "      \"client_secret_basic\"\n"
                + "   ],\n"
                + "   \"jwks_uri\":\"" + baseURI + "/keys\",\n"
                + "   \"response_modes_supported\":[\n"
                + "      \"query\",\n"
                + "      \"fragment\",\n"
                + "      \"form_post\"\n"
                + "   ],\n"
                + "   \"subject_types_supported\":[\n"
                + "      \"pairwise\"\n"
                + "   ],\n"
                + "   \"id_token_signing_alg_values_supported\":[\n"
                + "      \"RS256\"\n"
                + "   ],\n"
                + "   \"response_types_supported\":[\n"
                + "      \"code\",\n"
                + "      \"id_token\",\n"
                + "      \"code id_token\",\n"
                + "      \"id_token token\"\n"
                + "   ],\n"
                + "   \"scopes_supported\":[\n"
                + "      \"openid\",\n"
                + "      \"profile\",\n"
                + "      \"email\",\n"
                + "      \"offline_access\"\n"
                + "   ],\n"
                + "   \"issuer\":\"" + baseURI + "/lightweight\",\n"
                + "   \"request_uri_parameter_supported\":false,\n"
                + "   \"userinfo_endpoint\":\"" + baseURI + "/userinfo\",\n"
                + "   \"authorization_endpoint\":\"" + baseURI + "/authorize\",\n"
                + "   \"device_authorization_endpoint\":\"" + baseURI + "/devicecode\",\n"
                + "   \"http_logout_supported\":true,\n"
                + "   \"frontchannel_logout_supported\":true,\n"
                + "   \"end_session_endpoint\":\"" + baseURI + "/logout\",\n"
                + "   \"claims_supported\":[\n"
                + "      \"sub\",\n"
                + "      \"iss\",\n"
                + "      \"aud\",\n"
                + "      \"exp\",\n"
                + "      \"iat\",\n"
                + "      \"auth_time\",\n"
                + "      \"acr\",\n"
                + "      \"nonce\",\n"
                + "      \"preferred_username\",\n"
                + "      \"name\",\n"
                + "      \"tid\",\n"
                + "      \"ver\",\n"
                + "      \"at_hash\",\n"
                + "      \"c_hash\",\n"
                + "      \"email\"\n"
                + "   ]\n"
                + "}";
        rc.response().putHeader("Content-Type", "application/json");
        rc.endAndForget(data);
    }

    /*
     * First request:
     * GET
     * https://localhost:X/authorize?response_type=code&client_id=SECRET&scope=openid+openid+
     * email+profile&redirect_uri=http://localhost:8080/Login/oidcLoginSuccess&state=STATE
     *
     * returns a 302 to
     * GET http://localhost:8080/Login/oidcLoginSuccess?code=CODE&state=STATE
     */
    private void authorize(RoutingContext rc) {
        String response_type = rc.request().params().get("response_type");
        String clientId = rc.request().params().get("client_id");
        String scope = rc.request().params().get("scope");
        String state = rc.request().params().get("state");
        String redirect_uri = rc.request().params().get("redirect_uri");
        UUID code = UUID.randomUUID();
        URI redirect;
        try {
            redirect = new URI(redirect_uri + "?state=" + state);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        StringBuilder predefinedUsers = new StringBuilder();
        for (String predefinedUser : getUsers()) {
            predefinedUsers.append("   <button name='predefined' class='link' type='submit' value='")
                    .append(predefinedUser)
                    .append("' title='Log in as ")
                    .append(predefinedUser)
                    .append(" with roles: ")
                    .append(String.join(",", getUserRoles(predefinedUser)))
                    .append("'>")
                    .append(predefinedUser)
                    .append("</button>\n");
        }
        rc.response()
                .endAndForget("<html>"
                        + " <head>"
                        + "  <title>Login</title>"
                        + "  <style>"
                        + "        body {\n"
                        + "        display: flex;\n"
                        + "        flex-direction: column;\n"
                        + "        background-color: hsla(210, 10%, 23%, 1.0);\n"
                        + "        color: hsla(214, 96%, 96%, 0.9);\n"
                        + "        height: 100vh;\n"
                        + "        align-items: center;\n"
                        + "        justify-content: center;\n"
                        + "        margin: 0px;\n"
                        + "        font-family: -apple-system, BlinkMacSystemFont, 'Roboto', 'Segoe UI', Helvetica, Arial, sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol';\n"
                        + "      }\n"
                        + "      .card {\n"
                        + "        display: flex;\n"
                        + "        flex-direction: column;\n"
                        + "        justify-content: space-between;\n"
                        + "        border: 1px solid hsla(214, 60%, 80%, 0.14);\n"
                        + "        border-radius: 4px;\n"
                        + "        width: 400px;\n"
                        + "        filter: brightness(90%);\n"
                        + "      }\n"
                        + "      .card-header {\n"
                        + "        font-size: 1.125rem;\n"
                        + "        line-height: 1;\n"
                        + "        height: 25px;\n"
                        + "        display: flex;\n"
                        + "        flex-direction: row;\n"
                        + "        justify-content: space-between;\n"
                        + "        align-items: center;\n"
                        + "        padding: 10px 10px;\n"
                        + "        background-color: hsla(214, 65%, 85%, 0.06);\n"
                        + "        border-bottom: 1px solid hsla(214, 60%, 80%, 0.14);\n"
                        + "      }\n"
                        + "      .card-body {\n"
                        + "        line-height: 1;\n"
                        + "        display: flex;\n"
                        + "        flex-direction: column;\n"
                        + "        justify-content: space-between;\n"
                        + "        padding: 10px 10px;\n"
                        + "        gap: 10px;\n"
                        + "      }\n"
                        + "      .card:hover {\n"
                        + "        box-shadow: 0 4px 8px 0 rgba(0,0,0,0.2);\n"
                        + "      }\n"
                        + "      .predefined-form {\n"
                        + "        display: flex;\n"
                        + "        flex-direction: column;\n"
                        + "        align-items: flex-start;\n"
                        + "        gap: 10px;\n"
                        + "        \n"
                        + "      }\n"
                        + "      .link {\n"
                        + "        background: none!important;\n"
                        + "        border: none;\n"
                        + "        color: hsla(214, 96%, 96%, 0.9);\n"
                        + "        padding: 0!important;\n"
                        + "        text-decoration: none;\n"
                        + "        cursor: pointer;\n"
                        + "        font-size: large;\n"
                        + "      }\n"
                        + "      .link:hover {\n"
                        + "        filter: brightness(90%);\n"
                        + "      }\n"
                        + "      .custom-link{\n"
                        + "        display: flex;\n"
                        + "        font-size: large;\n"
                        + "        padding-top: 4px;\n"
                        + "        cursor: pointer;\n"
                        + "      }\n"
                        + "      .custom-form {\n"
                        + "        display: flex;\n"
                        + "        flex-direction: column;\n"
                        + "        gap: 5px;\n"
                        + "        padding-top: 5px;\n"
                        + "      }\n"
                        + "      .custom-button {\n"
                        + "        background: hsla(145, 65%, 42%, 0.5);\n"
                        + "        border: unset;\n"
                        + "        color: hsla(214, 96%, 96%, 0.9);\n"
                        + "        font-size: large;\n"
                        + "        cursor: pointer;\n"
                        + "      }\n"
                        + "      .custom-button:hover {\n"
                        + "        filter: brightness(90%);\n"
                        + "      }\n"
                        + "  </style>\n"
                        + " </head>\n"
                        + " <body>\n"
                        + "  <div class='card'>\n"
                        + "   <div class='card-header'>\n"
                        + "    <div>Login</div>\n"
                        + "   </div>\n"
                        + "   <div class='card-body'>\n"
                        + "    <form class='predefined-form' action='/login' method='post'>\n"
                        + "     <input type='hidden' name='redirect_uri' value='" + redirect.toASCIIString() + "'>\n"
                        + predefinedUsers
                        + "    </form>\n"
                        + "    <details>\n"
                        + "     <summary class='custom-link'>Custom user</summary>\n"
                        + "     <form class='custom-form' action='/login' method='post'>\n"
                        + "      <input type='hidden' name='redirect_uri' value='" + redirect.toASCIIString() + "'>"
                        + "      <input type='text' name='name' placeholder='Name'><br/>"
                        + "      <input type='text' name='roles' placeholder='Roles (comma-separated)'><br/>"
                        + "      <button class='custom-button' type='submit'>Login</button>\n"
                        + "     </form>\n"
                        + "    </details>\n"
                        + "   </div>\n"
                        + "  </div>\n"
                        + " </body>\n"
                        + "</html>");
    }

    private void login(RoutingContext rc) {
        String redirect_uri = rc.request().params().get("redirect_uri");
        String predefined = rc.request().params().get("predefined");
        String name = rc.request().params().get("name");
        String roles = rc.request().params().get("roles");
        if (predefined != null) {
            name = predefined;
            roles = String.join(",", getUserRoles(name));
        }
        if (name == null || name.isBlank()) {
            name = "user";
        }
        // store user|roles in the code param as Base64
        String code = Base64.getUrlEncoder().encodeToString((name + "|" + roles).getBytes(StandardCharsets.UTF_8));
        rc.response()
                .putHeader("Location", redirect_uri + "&code=" + code)
                .setStatusCode(302)
                .endAndForget();
    }

    /*
     * OIDC calls POST /token
     * grant_type=authorization_code
     * &code=CODE
     * &redirect_uri=URI
     *
     * returns:
     *
     * {
     * "token_type":"Bearer",
     * "scope":"openid email profile",
     * "expires_in":3600,
     * "ext_expires_in":3600,
     * "access_token":TOKEN,
     * "id_token":JWT
     * }
     *
     * ID token:
     * {
     * "ver": "2.0",
     * "iss": "http://localhost/lightweight",
     * "sub": "USERID",
     * "aud": "CLIENTID",
     * "exp": 1641906214,
     * "iat": 1641819514,
     * "nbf": 1641819514,
     * "name": "Foo Bar",
     * "preferred_username": "user@example.com",
     * "oid": "OPAQUE",
     * "email": "user@example.com",
     * "tid": "TENANTID",
     * "aio": "AZURE_OPAQUE"
     * }
     */
    private void accessTokenJson(RoutingContext rc) {
        String authorization_code = rc.request().formAttributes().get("authorization_code");
        String code = rc.request().formAttributes().get("code");
        String redirect_uri = rc.request().formAttributes().get("redirect_uri");
        String decodedCode = new String(Base64.getUrlDecoder().decode(code), StandardCharsets.UTF_8);
        int separator = decodedCode.indexOf('|');
        String user = decodedCode.substring(0, separator);
        String rolesAsString = decodedCode.substring(separator + 1);
        Set<String> roles = new HashSet<>(Arrays.asList(rolesAsString.split("[,\\s]+")));

        String accessToken = Jwt.claims()
                .expiresIn(Duration.ofDays(1))
                .issuedAt(Instant.now())
                .issuer(baseURI + "/lightweight")
                .subject(user)
                .upn(user)
                // not sure if the next three are even used
                .claim("name", "Foo Bar")
                .claim(Claims.preferred_username, user + "@example.com")
                .claim(Claims.email, user + "@example.com")
                .groups(roles)
                .jws()
                .keyId("KEYID")
                .sign(kp.getPrivate());
        String idToken = Jwt.claims()
                .expiresIn(Duration.ofDays(1))
                .issuedAt(Instant.now())
                .issuer(baseURI + "/lightweight")
                .audience(clientId)
                .subject(user)
                .upn(user)
                .claim("name", "Foo Bar")
                .claim(Claims.preferred_username, user + "@example.com")
                .claim(Claims.email, user + "@example.com")
                .groups(roles)
                .jws()
                .keyId("KEYID")
                .sign(kp.getPrivate());

        String data = "{\n"
                + " \"token_type\":\"Bearer\",\n"
                + " \"scope\":\"openid email profile\",\n"
                + " \"expires_in\":3600,\n"
                + " \"ext_expires_in\":3600,\n"
                + " \"access_token\":\"" + accessToken + "\",\n"
                + " \"id_token\":\"" + idToken + "\"\n"
                + " }  ";
        rc.response()
                .putHeader("Content-Type", "application/json")
                .endAndForget(data);
    }

    /*
     * {"kty":"RSA",
     * "use":"sig",
     * "kid":"KEYID",
     * "x5t":"KEYID",
     * "n":
     * "<MODULUS>",
     * "e":"<EXPONENT>",
     * "x5c":[
     * "KEYID"
     * ],
     * "issuer":"http://localhost/lightweight"},
     */
    private void getKeys(RoutingContext rc) {
        RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
        String modulus = Base64.getUrlEncoder().encodeToString(pub.getModulus().toByteArray());
        String exponent = Base64.getUrlEncoder().encodeToString(pub.getPublicExponent().toByteArray());
        String data = "{\n"
                + "  \"keys\": [\n"
                + "    {\n"
                + "      \"alg\": \"RS256\",\n"
                + "      \"kty\": \"RSA\",\n"
                + "      \"n\": \"" + modulus + "\",\n"
                + "      \"use\": \"sig\",\n"
                + "      \"kid\": \"KEYID\",\n"
                + "      \"k5t\": \"KEYID\",\n"
                + "      \"issuer\": \"" + baseURI + "/lightweight\",\n"
                + "      \"e\": \"" + exponent + "\"\n"
                + "    },\n"
                + "  ]\n"
                + "}";
        rc.response()
                .putHeader("Content-Type", "application/json")
                .endAndForget(data);
    }

    /*
     * /logout
     * ?post_logout_redirect_uri=URI
     * &id_token_hint=SECRET
     */
    private void logout(RoutingContext rc) {
        // we have no cookie state
        String redirect_uri = rc.request().params().get("post_logout_redirect_uri");
        rc.response()
                .putHeader("Location", redirect_uri)
                .setStatusCode(302)
                .endAndForget();
    }
}
