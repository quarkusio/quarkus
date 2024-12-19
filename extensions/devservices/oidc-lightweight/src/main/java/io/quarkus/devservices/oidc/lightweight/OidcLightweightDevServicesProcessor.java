package io.quarkus.devservices.oidc.lightweight;

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
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.stream.Collectors;

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
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.jwt.build.Jwt;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.handler.BodyHandler;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class OidcLightweightDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(OidcLightweightDevServicesProcessor.class);

    private static final String CONFIG_PREFIX = "quarkus.oidc.";
    private static final String OIDC_ENABLED = CONFIG_PREFIX + "enabled";
    private static final String TENANT_ENABLED_CONFIG_KEY = CONFIG_PREFIX + "tenant-enabled";
    private static final String AUTH_SERVER_URL_CONFIG_KEY = CONFIG_PREFIX + "auth-server-url";
    private static final String PROVIDER_CONFIG_KEY = CONFIG_PREFIX + "provider";
    private static final String APPLICATION_TYPE_CONFIG_KEY = CONFIG_PREFIX + "application-type";
    private static final String CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";
    private static final String CLIENT_SECRET_CONFIG_KEY = CONFIG_PREFIX + "credentials.secret";

    private static volatile OidcLightweightDevServicesConfig capturedDevServicesConfiguration;
    private static volatile boolean first = true;

    private static volatile KeyPair kp;
    private static volatile String baseURI;
    private static volatile String clientId;
    private static volatile String clientSecret;
    private static volatile String applicationType;
    private static volatile Map<String, String> configProperties;
    private static volatile int port;
    private static volatile Vertx vertx;
    private static volatile HttpServer httpServer;

    @BuildStep
    public DevServicesResultBuildItem startLightweightServer(CuratedApplicationShutdownBuildItem closeBuildItem,
            OidcLightweightDevServicesConfig devServicesConfig,
            BuildProducer<OidcLightweightDevServicesConfigBuildItem> lightweightBuildItemBuildProducer) {
        if (shouldNotStartLightweightServer(devServicesConfig)) {
            closeDevService();
            return null;
        }

        capturedDevServicesConfiguration = devServicesConfig;
        if (vertx == null || httpServer == null) {
            LOG.info("Starting OIDC Lightweight Dev Services");
            if (vertx == null) {
                vertx = Vertx.vertx();
            }
            HttpServerOptions options = new HttpServerOptions();
            options.setPort(0);
            httpServer = vertx.createHttpServer(options);

            Router router = Router.router(vertx);
            httpServer.requestHandler(router);
            registerRoutes(router);

            httpServer.listenAndAwait();
            port = httpServer.actualPort();
            LOG.infof("OIDC Lightweight Dev Services started on %s", baseURI);
        }

        if (first) {
            first = false;
            closeBuildItem.addCloseTask(this::closeDevService, true);
        }

        RunningDevService newDevService = createRunningDevService(lightweightBuildItemBuildProducer);
        return newDevService.toBuildItem();
    }

    private void closeDevService() {
        if (httpServer != null) {
            try {
                httpServer.closeAndAwait();
            } catch (Throwable t) {
                LOG.error("Failed to close HTTP Server", t);
            }
            httpServer = null;
        }
        if (vertx != null) {
            try {
                vertx.closeAndAwait();
            } catch (Throwable t) {
                LOG.error("Failed to close Vertx instance", t);
            }
            vertx = null;
        }
        port = -1;
        clientId = null;
        applicationType = null;
        capturedDevServicesConfiguration = null;
        first = true;
    }

    private static boolean shouldNotStartLightweightServer(OidcLightweightDevServicesConfig devServicesConfig) {
        if (!devServicesConfig.enabled()) {
            // explicitly disabled
            LOG.debug("Not starting OIDC Lightweight Dev Services as it has been disabled in the config");
            return true;
        }
        if (!isOidcEnabled()) {
            LOG.debug("Not starting OIDC Lightweight Dev Services as OIDC extension has been disabled in the config");
            return true;
        }
        if (!isOidcTenantEnabled()) {
            LOG.debug("Not starting OIDC Lightweight Dev Services as 'quarkus.oidc.tenant.enabled' is false");
            return true;
        }
        if (ConfigUtils.isPropertyPresent(AUTH_SERVER_URL_CONFIG_KEY)) {
            LOG.debug("Not starting OIDC Lightweight Dev Services as 'quarkus.oidc.auth-server-url' has been provided");
            return true;
        }
        if (ConfigUtils.isPropertyPresent(PROVIDER_CONFIG_KEY)) {
            LOG.debug("Not starting OIDC Lightweight Dev Services as 'quarkus.oidc.provider' has been provided");
            return true;
        }
        return false;
    }

    private RunningDevService createRunningDevService(
            BuildProducer<OidcLightweightDevServicesConfigBuildItem> lightweightBuildItemBuildProducer) {
        if (!getOidcClientId().equals(clientId) || !getOidcApplicationType().equals(applicationType)) {
            // relevant configuration has changed
            baseURI = "http://localhost:" + port;
            clientId = getOidcClientId();
            clientSecret = getOidcClientSecret();
            applicationType = getOidcApplicationType();
            final Map<String, String> aConfigProperties = new HashMap<>();
            aConfigProperties.put(AUTH_SERVER_URL_CONFIG_KEY, baseURI);
            aConfigProperties.put(APPLICATION_TYPE_CONFIG_KEY, applicationType);
            aConfigProperties.put(CLIENT_ID_CONFIG_KEY, clientId);
            aConfigProperties.put(CLIENT_SECRET_CONFIG_KEY, clientSecret);
            configProperties = Map.copyOf(aConfigProperties);
        }
        lightweightBuildItemBuildProducer.produce(new OidcLightweightDevServicesConfigBuildItem(configProperties));
        return new RunningDevService("oidc-lightweight", null, () -> {
        }, configProperties);
    }

    private void registerRoutes(Router router) {
        BodyHandler bodyHandler = BodyHandler.create();
        router.get("/").handler(this::mainRoute);
        router.get("/.well-known/openid-configuration").handler(this::configuration);
        router.get("/authorize").handler(this::authorize);
        router.post("/login").handler(bodyHandler).handler(this::login);
        router.post("/token").handler(bodyHandler).handler(this::token);
        router.get("/keys").handler(this::getKeys);
        router.get("/logout").handler(this::logout);
        router.get("/userinfo").handler(this::userInfo);

        // can be used for testing of bearer token authentication
        router.get("/testing/generate/access-token").handler(this::generateAccessToken);

        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        kpg.initialize(2048);
        kp = kpg.generateKeyPair();
    }

    private void generateAccessToken(RoutingContext rc) {
        String user = rc.request().getParam("user");
        if (user == null || user.isEmpty()) {
            rc.response().setStatusCode(400).endAndForget("Missing required parameter: user");
            return;
        }
        String rolesParam = rc.request().getParam("roles");
        Set<String> roles = new HashSet<>();
        if (rolesParam == null || rolesParam.isEmpty()) {
            roles.addAll(getUserRoles(user));
        } else {
            roles.addAll(Arrays.asList(rolesParam.split(",")));
        }
        rc.response().endAndForget(createAccessToken(user, roles, Set.of("openid", "email")));
    }

    private List<String> getUsers() {
        if (capturedDevServicesConfiguration.roles().isEmpty()) {
            return Arrays.asList("alice", "bob");
        } else {
            List<String> ret = new ArrayList<>(capturedDevServicesConfiguration.roles().keySet());
            Collections.sort(ret);
            return ret;
        }
    }

    private List<String> getUserRoles(String user) {
        List<String> roles = capturedDevServicesConfiguration.roles().get(user);
        return roles == null ? ("alice".equals(user) ? List.of("admin", "user") : List.of("user"))
                : roles;
    }

    private static boolean isOidcEnabled() {
        return ConfigProvider.getConfig().getValue(OIDC_ENABLED, Boolean.class);
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
                .orElseGet(() -> UUID.randomUUID().toString());
    }

    private void mainRoute(RoutingContext rc) {
        rc.response().endAndForget("OIDC Lightweight server up and running");
    }

    private void configuration(RoutingContext rc) {
        String data = """
                {
                   "token_endpoint":"%1$s/token",
                   "token_endpoint_auth_methods_supported":[
                      "client_secret_post",
                      "private_key_jwt",
                      "client_secret_basic"
                   ],
                   "jwks_uri":"%1$s/keys",
                   "response_modes_supported":[
                      "query"
                   ],
                   "subject_types_supported":[
                      "pairwise"
                   ],
                   "id_token_signing_alg_values_supported":[
                      "RS256"
                   ],
                   "response_types_supported":[
                      "code",
                      "id_token",
                      "code id_token",
                      "id_token token",
                      "code id_token token"
                   ],
                   "scopes_supported":[
                      "openid",
                      "profile",
                      "email",
                      "offline_access"
                   ],
                   "issuer":"%1$s/lightweight",
                   "request_uri_parameter_supported":false,
                   "userinfo_endpoint":"%1$s/userinfo",
                   "authorization_endpoint":"%1$s/authorize",
                   "device_authorization_endpoint":"%1$s/devicecode",
                   "http_logout_supported":true,
                   "frontchannel_logout_supported":true,
                   "end_session_endpoint":"%1$s/logout",
                   "claims_supported":[
                      "sub",
                      "iss",
                      "aud",
                      "exp",
                      "iat",
                      "auth_time",
                      "acr",
                      "nonce",
                      "preferred_username",
                      "name",
                      "tid",
                      "ver",
                      "at_hash",
                      "c_hash",
                      "email"
                   ]
                }
                """.formatted(baseURI);
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
        URI redirect;
        try {
            redirect = new URI(redirect_uri + "?state=" + state);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        StringBuilder predefinedUsers = new StringBuilder();
        for (String predefinedUser : getUsers()) {
            predefinedUsers.append("   <button name='predefined-" + predefinedUser + "' class='link' type='submit' value='")
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
                .endAndForget(
                        """
                                <html>
                                 <head>
                                  <title>Login</title>
                                  <style>
                                        body {
                                        display: flex;
                                        flex-direction: column;
                                        background-color: hsla(210, 10%, 23%, 1.0);
                                        color: hsla(214, 96%, 96%, 0.9);
                                        height: 100vh;
                                        align-items: center;
                                        justify-content: center;
                                        margin: 0px;
                                        font-family: -apple-system, BlinkMacSystemFont, 'Roboto', 'Segoe UI', Helvetica, Arial, sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol';
                                      }
                                      .card {
                                        display: flex;
                                        flex-direction: column;
                                        justify-content: space-between;
                                        border: 1px solid hsla(214, 60%, 80%, 0.14);
                                        border-radius: 4px;
                                        width: 400px;
                                        filter: brightness(90%);
                                      }
                                      .card-header {
                                        font-size: 1.125rem;
                                        line-height: 1;
                                        height: 25px;
                                        display: flex;
                                        flex-direction: row;
                                        justify-content: space-between;
                                        align-items: center;
                                        padding: 10px 10px;
                                        background-color: hsla(214, 65%, 85%, 0.06);
                                        border-bottom: 1px solid hsla(214, 60%, 80%, 0.14);
                                      }
                                      .card-body {
                                        line-height: 1;
                                        display: flex;
                                        flex-direction: column;
                                        justify-content: space-between;
                                        padding: 10px 10px;
                                        gap: 10px;
                                      }
                                      .card:hover {
                                        box-shadow: 0 4px 8px 0 rgba(0,0,0,0.2);
                                      }
                                      .predefined-form {
                                        display: flex;
                                        flex-direction: column;
                                        align-items: flex-start;
                                        gap: 10px;
                                      }
                                      .link {
                                        background: none!important;
                                        border: none;
                                        color: hsla(214, 96%, 96%, 0.9);
                                        padding: 0!important;
                                        text-decoration: none;
                                        cursor: pointer;
                                        font-size: large;
                                      }
                                      .link:hover {
                                        filter: brightness(90%);
                                      }
                                      .custom-link{
                                        display: flex;
                                        font-size: large;
                                        padding-top: 4px;
                                        cursor: pointer;
                                      }
                                      .custom-form {
                                        display: flex;
                                        flex-direction: column;
                                        gap: 5px;
                                        padding-top: 5px;
                                      }
                                      .custom-button {
                                        background: hsla(145, 65%, 42%, 0.5);
                                        border: unset;
                                        color: hsla(214, 96%, 96%, 0.9);
                                        font-size: large;
                                        cursor: pointer;
                                      }
                                      .custom-button:hover {
                                        filter: brightness(90%);
                                      }
                                  </style>
                                 </head>
                                 <body>
                                  <div class='card'>
                                   <div class='card-header'>
                                    <div>Login</div>
                                   </div>
                                   <div class='card-body'>
                                    <form class='predefined-form' action='/login' method='post'>
                                """
                                + """
                                            <input type='hidden' name='redirect_uri' value='%1$s'>
                                            <input type='hidden' name='response_type' value='%3$s'>
                                            <input type='hidden' name='client_id' value='%4$s'>
                                            <input type='hidden' name='scope' value='%5$s'>
                                                %2$s
                                            </form>
                                            <details>
                                             <summary class='custom-link'>Custom user</summary>
                                             <form class='custom-form' action='/login' method='post'>
                                              <input type='hidden' name='redirect_uri' value='%1$s'>
                                              <input type='hidden' name='response_type' value='%3$s'>
                                              <input type='hidden' name='client_id' value='%4$s'>
                                              <input type='hidden' name='scope' value='%5$s'>
                                              <input type='text' name='name' placeholder='Name'><br/>
                                              <input type='text' name='roles' placeholder='Roles (comma-separated)'><br/>
                                              <button class='custom-button' type='submit' name='login'>Login</button>
                                             </form>
                                            </details>
                                           </div>
                                          </div>
                                         </body>
                                        </html>
                                        """.formatted(redirect.toASCIIString(), predefinedUsers, response_type, clientId,
                                        scope));
    }

    private void login(RoutingContext rc) {
        String redirect_uri = rc.request().params().get("redirect_uri");
        String predefined = null;
        for (Map.Entry<String, String> param : rc.request().params()) {
            if (param.getKey().startsWith("predefined")) {
                predefined = param.getValue();
                break;
            }
        }
        String name = rc.request().params().get("name");
        String roles = rc.request().params().get("roles");
        String scope = rc.request().params().get("scope");
        String clientId = rc.request().params().get("client_id");
        String responseType = rc.request().params().get("response_type");

        if (predefined != null) {
            name = predefined;
            roles = String.join(",", getUserRoles(name));
        }
        if (name == null || name.isBlank()) {
            name = "user";
        }

        if (responseType == null || responseType.isEmpty()) {
            rc.response().setStatusCode(500).endAndForget("Illegal state - the 'response_type' parameter is required");
            return;
        }

        StringBuilder queryParams = new StringBuilder();

        if (responseType.contains("code")) {
            String code = new UserAndRoles(name, roles).encode();
            queryParams.append("&code=").append(code);
        }

        if (responseType.contains("idtoken")) {
            String idToken = createIdToken(name, getUserRolesSet(roles), clientId);
            queryParams.append("&id_token=").append(idToken);
        }

        if (responseType.contains(" token")) {
            String accessToken = createAccessToken(name, getUserRolesSet(roles), getScopeAsSet(scope));
            queryParams.append("&access_token=").append(accessToken);
        }

        rc.response()
                .putHeader("Location", redirect_uri + queryParams)
                .setStatusCode(302)
                .endAndForget();
    }

    private void token(RoutingContext rc) {
        String grantType = rc.request().formAttributes().get("grant_type");
        switch (grantType) {
            case "authorization_code" -> authorizationCodeFlowTokenEndpoint(rc);
            case "refresh_token" -> refreshTokenEndpoint(rc);
            default -> rc.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .putHeader("Cache-Control", "no-store")
                    .endAndForget("Unsupported grant type: " + grantType);
        }
    }

    private void refreshTokenEndpoint(RoutingContext rc) {
        String clientId = rc.request().formAttributes().get("client_id");
        String clientSecret = rc.request().formAttributes().get("client_secret");
        String scope = rc.request().formAttributes().get("scope");
        if (!OidcLightweightDevServicesProcessor.clientId.equals(clientId)) {
            LOG.warn("Client ID does not match, denying token refresh");
            invalidTokenResponse(rc);
            return;
        }
        if (!OidcLightweightDevServicesProcessor.clientSecret.equals(clientSecret)) {
            LOG.warn("Client secret does not match, denying token refresh");
            invalidTokenResponse(rc);
            return;
        }
        String refreshToken = rc.request().formAttributes().get("refresh_token");
        UserAndRoles userAndRoles = decode(refreshToken);
        if (userAndRoles == null) {
            LOG.warn("Received invalid refresh token, denying token refresh");
            invalidTokenResponse(rc);
            return;
        }

        String accessToken = createAccessToken(userAndRoles.user, userAndRoles.getRolesAsSet(), getScopeAsSet(scope));
        String data = """
                {
                   "access_token": "%s",
                   "token_type": "Bearer",
                   "refresh_token": "%s",
                   "expires_in": 3600
                }
                """.formatted(accessToken, refreshToken);
        rc.response()
                .putHeader("Content-Type", "application/json")
                .putHeader("Cache-Control", "no-store")
                .endAndForget(data);
    }

    /*
     * OIDC calls POST /token?
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
    private void authorizationCodeFlowTokenEndpoint(RoutingContext rc) {
        // TODO: check redirect_uri is same as in the initial Authorization Request
        String clientId = rc.request().formAttributes().get("client_id");
        if (clientId == null || clientId.isEmpty()) {
            clientId = OidcLightweightDevServicesProcessor.clientId;
        }
        String scope = rc.request().formAttributes().get("scope");

        String code = rc.request().formAttributes().get("code");
        UserAndRoles userAndRoles = decode(code);
        if (userAndRoles == null) {
            invalidTokenResponse(rc);
            return;
        }

        String accessToken = createAccessToken(userAndRoles.user, userAndRoles.getRolesAsSet(), getScopeAsSet(scope));
        String idToken = createIdToken(userAndRoles.user, userAndRoles.getRolesAsSet(), clientId);

        String data = """
                {
                 "token_type":"Bearer",
                 "scope":"openid email profile",
                 "expires_in":3600,
                 "ext_expires_in":3600,
                 "access_token":"%s",
                 "id_token":"%s",
                 "refresh_token": "%s"
                 }
                """.formatted(accessToken, idToken, userAndRoles.encode());
        rc.response()
                .putHeader("Content-Type", "application/json")
                .putHeader("Cache-Control", "no-store")
                .endAndForget(data);
    }

    private static void invalidTokenResponse(RoutingContext rc) {
        rc.response()
                .setStatusCode(400)
                .putHeader("Content-Type", "application/json")
                .putHeader("Cache-Control", "no-store")
                .endAndForget("""
                        {
                           "error": "invalid_request"
                        }
                        """);
    }

    private static String createIdToken(String user, Set<String> roles, String clientId) {
        return Jwt.claims()
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
    }

    private static String createAccessToken(String user, Set<String> roles, Set<String> scope) {
        return Jwt.claims()
                .expiresIn(Duration.ofDays(1))
                .issuedAt(Instant.now())
                .issuer(baseURI + "/lightweight")
                .subject(user)
                .scope(scope)
                .upn(user)
                .claim("name", "Foo Bar")
                .claim(Claims.preferred_username, user + "@example.com")
                .claim(Claims.email, user + "@example.com")
                .groups(roles)
                .jws()
                .keyId("KEYID")
                .sign(kp.getPrivate());
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
        String data = """
                {
                  "keys": [
                    {
                      "alg": "RS256",
                      "kty": "RSA",
                      "n": "%s",
                      "use": "sig",
                      "kid": "KEYID",
                      "k5t": "KEYID",
                      "issuer": "%s/lightweight",
                      "e": "%s"
                    },
                  ]
                }
                """.formatted(modulus, baseURI, exponent);
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

    private void userInfo(RoutingContext rc) {
        var authorization = rc.request().getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length());
            JsonObject claims = decodeJwtContent(token);
            if (claims != null && claims.containsKey(Claims.preferred_username.name())) {
                String data = """
                        {
                            "preferred_username": "%s",
                            "sub": "%s",
                            "name": "Foo Bar",
                            "family_name": "Foo",
                            "given_name": "Bar",
                            "email": "%s"
                        }
                        """.formatted(claims.getString(Claims.preferred_username.name()),
                        claims.getString(Claims.sub.name()), claims.getString(Claims.email.name()));
                rc.response()
                        .putHeader("Content-Type", "application/json")
                        .endAndForget(data);
                return;
            }
        }
        rc.response().setStatusCode(401).endAndForget("WWW-Authenticate: Bearer error=\"invalid_token\"");
    }

    private UserAndRoles decode(String encodedContent) {
        if (encodedContent != null && !encodedContent.isEmpty()) {
            String decodedCode = new String(Base64.getUrlDecoder().decode(encodedContent), StandardCharsets.UTF_8);
            int separator = decodedCode.indexOf('|');
            if (separator != -1) {
                String user = decodedCode.substring(0, separator);
                String roles = decodedCode.substring(separator + 1);
                if (roles.isBlank()) {
                    roles = String.join(",", getUserRoles(user));
                }
                return new UserAndRoles(user, roles);
            } else if (getUsers().contains(decodedCode)) {
                String roles = String.join(",", getUserRoles(decodedCode));
                return new UserAndRoles(decodedCode, roles);
            }
        }
        return null;
    }

    private static JsonObject decodeJwtContent(String jwt) {
        String encodedContent = getJwtContentPart(jwt);
        if (encodedContent == null) {
            return null;
        }
        return decodeAsJsonObject(encodedContent);
    }

    private static String getJwtContentPart(String jwt) {
        StringTokenizer tokens = new StringTokenizer(jwt, ".");
        // part 1: skip the token headers
        tokens.nextToken();
        if (!tokens.hasMoreTokens()) {
            return null;
        }
        // part 2: token content
        String encodedContent = tokens.nextToken();

        // let's check only 1 more signature part is available
        if (tokens.countTokens() != 1) {
            return null;
        }
        return encodedContent;
    }

    private static String base64UrlDecode(String encodedContent) {
        return new String(Base64.getUrlDecoder().decode(encodedContent), StandardCharsets.UTF_8);
    }

    private static JsonObject decodeAsJsonObject(String encodedContent) {
        try {
            return new JsonObject(base64UrlDecode(encodedContent));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Set<String> getUserRolesSet(String roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(roles.split(",")).map(String::trim).collect(Collectors.toSet());
    }

    private static Set<String> getScopeAsSet(String scope) {
        if (scope == null || scope.isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(scope.split(" ")).collect(Collectors.toSet());
    }

    private record UserAndRoles(String user, String roles) {

        private String encode() {
            // store user|roles in the code param as Base64
            return Base64.getUrlEncoder().encodeToString((user + "|" + roles).getBytes(StandardCharsets.UTF_8));
        }

        private Set<String> getRolesAsSet() {
            if (roles == null || roles.isEmpty()) {
                return Set.of();
            } else {
                return new HashSet<>(Arrays.asList(roles.split("[,\\s]+")));
            }
        }

    }

}
