package io.quarkus.oidc.runtime;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.vertx.http.runtime.security.ImmutablePathMatcher;
import io.smallrye.jwt.util.KeyUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Singleton
public final class ClientIdMetadataHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(ClientIdMetadataHandler.class);
    private static final String HTTPS_SCHEME = "https://";
    private static final String HTTP_SCHEME = "http://";
    private static final String CLIENT_NAME = "client_name";
    private static final String REDIRECT_URIS = "redirect_uris";
    private static final String TOKEN_ENDPOINT_AUTH_METHOD = "token_endpoint_auth_method";
    private static final String JWKS = "jwks";

    private final DefaultTenantConfigResolver resolver;
    private volatile ImmutablePathMatcher<Handler<RoutingContext>> pathMatcher;

    record NewClientIdMetadata() {
    }

    ClientIdMetadataHandler(DefaultTenantConfigResolver resolver) {
        this.resolver = resolver;
        this.pathMatcher = null;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        var matcher = pathMatcher;
        if (matcher != null && HttpMethod.GET.equals(routingContext.request().method())) {
            Handler<RoutingContext> routeHandler = matcher.match(routingContext.normalizedPath()).getValue();
            if (routeHandler != null) {
                routeHandler.handle(routingContext);
                return;
            }
        }

        routingContext.next();
    }

    void setup(@Observes Router router) {
        createOrUpdatePathMatcher();
    }

    synchronized void updatePathMatcher(@Observes NewClientIdMetadata ignored) {
        createOrUpdatePathMatcher();
    }

    private void createOrUpdatePathMatcher() {
        ImmutablePathMatcher.ImmutablePathMatcherBuilder<Handler<RoutingContext>> builder = null;
        Map<String, OidcTenantConfig> pathCache = null;
        for (TenantConfigContext configContext : resolver.getTenantConfigBean().getAllTenantConfigs()) {
            if (configContext.ready() && configContext.oidcConfig().tenantEnabled()
                    && isClientIdMetadataUrl(configContext.oidcConfig())) {
                if (builder == null) {
                    builder = ImmutablePathMatcher.builder();
                    pathCache = new HashMap<>();
                }
                String clientId = configContext.oidcConfig().clientId().get();
                String routePath = URI.create(clientId).getRawPath();
                if (routePath.contains("*")) {
                    throw new IllegalStateException(
                            "Client ID metadata document path cannot contain a wildcard '*' character");
                }
                OidcTenantConfig previousConfig = pathCache.put(routePath, configContext.oidcConfig());
                if (previousConfig == null) {
                    Handler<RoutingContext> routeHandler = new RouteHandler(configContext.oidcConfig());
                    builder.addPath(routePath, routeHandler);
                } else {
                    String previousTenantId = previousConfig.tenantId().get();
                    String currentTenantId = configContext.oidcConfig().tenantId().get();
                    if (!previousTenantId.equals(currentTenantId)) {
                        String errorMessage = "OIDC tenants '%s' and '%s' share the same client ID metadata document path '%s', which is not supported"
                                .formatted(previousTenantId, currentTenantId, routePath);
                        LOG.error(errorMessage);
                        throw new OIDCException(errorMessage);
                    }
                }
            }
        }
        if (builder != null) {
            pathMatcher = builder.build();
        } else {
            pathMatcher = null;
        }
    }

    private static final class RouteHandler implements Handler<RoutingContext> {
        private final String metadataJson;

        RouteHandler(OidcTenantConfig oidcConfig) {
            this.metadataJson = prepareMetadata(oidcConfig);
        }

        @Override
        public void handle(RoutingContext context) {
            context.response()
                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .setStatusCode(200)
                    .end(metadataJson);
        }

        private static String prepareMetadata(OidcTenantConfig oidcConfig) {
            LOG.debugf("Preparing Client ID metadata document for tenant %s", oidcConfig.tenantId().get());
            JsonObject metadata = new JsonObject();

            metadata.put(OidcConstants.CLIENT_ID, oidcConfig.clientId().get());
            metadata.put(CLIENT_NAME, oidcConfig.clientName().get());

            JsonArray redirectUris = new JsonArray();
            redirectUris.add(OidcUtils.resolveRedirectUriForClientIdMetadata(oidcConfig));
            metadata.put(REDIRECT_URIS, redirectUris);

            PublicKey publicKey = loadPublicKey(oidcConfig);
            if (publicKey != null) {
                metadata.put(TOKEN_ENDPOINT_AUTH_METHOD, "private_key_jwt");
                metadata.put(JWKS, publicKeyToJwks(publicKey));
            } else {
                metadata.put(TOKEN_ENDPOINT_AUTH_METHOD, "none");
            }

            return metadata.toString();
        }

        private static PublicKey loadPublicKey(OidcTenantConfig oidcConfig) {
            var jwt = oidcConfig.credentials().jwt();
            if (!OidcCommonUtils.isJwtPrivateKeyConfigured(jwt)) {
                return null;
            }
            try {
                if (jwt.publicKey().isPresent()) {
                    return decodePublicKeyOrCertificate(jwt.publicKey().get());
                } else if (jwt.publicKeyFile().isPresent()) {
                    String content = Files.readString(Path.of(jwt.publicKeyFile().get()));
                    return decodePublicKeyOrCertificate(content);
                }
            } catch (Exception e) {
                throw new OIDCException("Failed to load the public key for tenant "
                        + oidcConfig.tenantId().get(), e);
            }
            return null;
        }

        private static PublicKey decodePublicKeyOrCertificate(String pem) throws Exception {
            if (pem.contains("BEGIN CERTIFICATE")) {
                return KeyUtils.decodeCertificate(pem);
            }
            return KeyUtils.decodePublicKey(pem);
        }

        private static JsonObject publicKeyToJwks(PublicKey publicKey) {
            try {
                PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(publicKey);
                jwk.setUse("sig");
                JsonObject jwkJson = new JsonObject(
                        jwk.toJson(org.jose4j.jwk.JsonWebKey.OutputControlLevel.PUBLIC_ONLY));
                JsonArray keys = new JsonArray();
                keys.add(jwkJson);
                return new JsonObject().put("keys", keys);
            } catch (JoseException e) {
                throw new OIDCException("Failed to convert the public key to JWK format", e);
            }
        }
    }

    static void fireClientIdMetadataChangedEvent(OidcTenantConfig oidcConfig, TenantConfigContext tenant) {
        OidcTenantConfig previousConfig = tenant.oidcConfig();
        boolean newIsMetadataUrl = isClientIdMetadataUrl(oidcConfig);
        boolean previousIsMetadataUrl = previousConfig != null && isClientIdMetadataUrl(previousConfig);

        final boolean changed;
        if (newIsMetadataUrl) {
            // The new config is a CIMD tenant: (re)register the path if the previous config was not a CIMD
            // tenant or if any of the properties the metadata document is built from have changed
            changed = !previousIsMetadataUrl || metadataChanged(oidcConfig, previousConfig);
        } else {
            // The new config is not a CIMD tenant: rebuild only if the previous config was one, so its
            // now-stale metadata path is removed and we do not keep serving invalid metadata
            changed = previousIsMetadataUrl;
        }
        if (changed) {
            fireClientIdMetadataEvent();
        }
    }

    static boolean metadataChanged(OidcTenantConfig oidcConfig, OidcTenantConfig previousConfig) {
        if (!oidcConfig.clientId().equals(previousConfig.clientId())
                || !oidcConfig.clientName().equals(previousConfig.clientName())
                || !oidcConfig.authentication().redirectPath().equals(previousConfig.authentication().redirectPath())) {
            return true;
        }

        var jwt = oidcConfig.credentials().jwt();
        var previousJwt = previousConfig.credentials().jwt();
        return !jwt.publicKey().equals(previousJwt.publicKey())
                || !jwt.publicKeyFile().equals(previousJwt.publicKeyFile());
    }

    static void fireClientIdMetadataReadyEvent(OidcTenantConfig oidcConfig) {
        if (isClientIdMetadataUrl(oidcConfig)) {
            fireClientIdMetadataEvent();
        }
    }

    private static void fireClientIdMetadataEvent() {
        Event<NewClientIdMetadata> event = Arc.container().beanManager().getEvent()
                .select(NewClientIdMetadata.class);
        event.fire(new NewClientIdMetadata());
    }

    static boolean isClientIdMetadataUrl(OidcTenantConfig oidcConfig) {
        if (!oidcConfig.clientId().isPresent()) {
            return false;
        }
        String clientId = oidcConfig.clientId().get();
        if (clientId.startsWith(HTTPS_SCHEME)) {
            return true;
        }
        return !oidcConfig.clientIdMetadata().forceHttpsScheme() && clientId.startsWith(HTTP_SCHEME);
    }
}
