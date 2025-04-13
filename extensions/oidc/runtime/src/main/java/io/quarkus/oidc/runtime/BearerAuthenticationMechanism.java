package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcAuthenticationPolicy.OAuth2StepUpAuthenticationPolicy.isInsufficientUserAuth;
import static io.quarkus.oidc.runtime.OidcUtils.extractBearerToken;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.OidcAuthenticationPolicy.OAuth2StepUpAuthenticationPolicy;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class BearerAuthenticationMechanism extends AbstractOidcAuthenticationMechanism {
    private static final Logger LOG = Logger.getLogger(BearerAuthenticationMechanism.class);

    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager, OidcTenantConfig oidcTenantConfig) {
        LOG.debug("Starting a bearer access token authentication");
        String token = extractBearerToken(context, oidcTenantConfig);
        // if a bearer token is provided try to authenticate
        if (token != null) {
            try {
                setCertificateThumbprint(context, oidcTenantConfig, token);
                setDPopProof(context, oidcTenantConfig, token);
            } catch (AuthenticationFailedException ex) {
                return Uni.createFrom().failure(ex);
            }
            return authenticate(identityProviderManager, context, new AccessTokenCredential(token));
        }
        LOG.debug("Bearer access token is not available");
        return Uni.createFrom().nullItem();
    }

    private static void setCertificateThumbprint(RoutingContext context, OidcTenantConfig oidcTenantConfig, String token) {
        if (oidcTenantConfig.token().binding().certificate()) {
            Certificate cert = getCertificate(context, token);
            if (!(cert instanceof X509Certificate)) {
                LOG.warn("Access token must be bound to X509 client certiifcate");
                throw new AuthenticationFailedException(tokenMap(token));
            }
            context.put(OidcConstants.X509_SHA256_THUMBPRINT,
                    TrustStoreUtils.calculateThumprint((X509Certificate) cert));
        }
    }

    private static void setDPopProof(RoutingContext context, OidcTenantConfig oidcTenantConfig, String token) {
        if (OidcConstants.DPOP_SCHEME.equals(oidcTenantConfig.token().authorizationScheme())) {

            List<String> proofs = context.request().headers().getAll(OidcConstants.DPOP_SCHEME);
            if (proofs == null || proofs.isEmpty()) {
                LOG.warn("DPOP proof header must be present to verify the DPOP access token binding");
                throw new AuthenticationFailedException(tokenMap(token));
            }
            if (proofs.size() != 1) {
                LOG.warn("Only a single DPOP proof header is accepted");
                throw new AuthenticationFailedException(tokenMap(token));
            }
            String proof = proofs.get(0);

            // Initial proof check:
            JsonObject proofJwtHeaders = OidcUtils.decodeJwtHeaders(proof);
            JsonObject proofJwtClaims = OidcCommonUtils.decodeJwtContent(proof);

            if (!OidcConstants.DPOP_TOKEN_TYPE.equals(proofJwtHeaders.getString(OidcConstants.TOKEN_TYPE_HEADER))) {
                LOG.warn("Invalid DPOP proof token type ('typ') header");
                throw new AuthenticationFailedException(tokenMap(token));
            }

            // Check HTTP method and request URI
            String proofHttpMethod = proofJwtClaims.getString(OidcConstants.DPOP_HTTP_METHOD);
            if (proofHttpMethod == null) {
                LOG.warn("DPOP proof HTTP method claim is missing");
                throw new AuthenticationFailedException(tokenMap(token));
            }

            String httpMethod = context.request().method().name();
            if (!httpMethod.equals(proofHttpMethod)) {
                LOG.warnf("DPOP proof HTTP method claim %s does not match the request HTTP method %s", proofHttpMethod,
                        httpMethod);
                throw new AuthenticationFailedException(tokenMap(token));
            }

            // Check HTTP request URI
            String proofHttpRequestUri = proofJwtClaims.getString(OidcConstants.DPOP_HTTP_REQUEST_URI);
            if (proofHttpRequestUri == null) {
                LOG.warn("DPOP proof HTTP request uri claim is missing");
                throw new AuthenticationFailedException(tokenMap(token));
            }

            String httpRequestUri = context.request().absoluteURI();
            int queryIndex = httpRequestUri.indexOf("?");
            if (queryIndex > 0) {
                httpRequestUri = httpRequestUri.substring(0, queryIndex);
            }
            if (!httpRequestUri.equals(proofHttpRequestUri)) {
                LOG.warnf("DPOP proof HTTP request uri claim %s does not match the request HTTP uri %s", proofHttpRequestUri,
                        httpRequestUri);
                throw new AuthenticationFailedException(tokenMap(token));
            }

            context.put(OidcUtils.DPOP_PROOF, proof);
            context.put(OidcUtils.DPOP_PROOF_JWT_HEADERS, proofJwtHeaders);
            context.put(OidcUtils.DPOP_PROOF_JWT_CLAIMS, proofJwtClaims);
        }
    }

    private static Certificate getCertificate(RoutingContext context, String token) {
        try {
            return context.request().sslSession().getPeerCertificates()[0];
        } catch (SSLPeerUnverifiedException e) {
            LOG.warn("Access token must be certificate bound but no client certificate is available");
            throw new AuthenticationFailedException(tokenMap(token));
        }
    }

    private static Map<String, Object> tokenMap(String token) {
        return Map.of(OidcConstants.ACCESS_TOKEN_VALUE, token);
    }

    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        Uni<TenantConfigContext> tenantContext = resolver.resolveContext(context);
        return tenantContext.onItem().transformToUni(new Function<TenantConfigContext, Uni<? extends ChallengeData>>() {
            @Override
            public Uni<ChallengeData> apply(TenantConfigContext tenantContext) {
                final String wwwAuthHeaderValue;
                if (isInsufficientUserAuth(context)) {
                    wwwAuthHeaderValue = tenantContext.oidcConfig().token().authorizationScheme() +
                            OAuth2StepUpAuthenticationPolicy.getAuthRequirementChallenge(context);
                } else {
                    wwwAuthHeaderValue = tenantContext.oidcConfig().token().authorizationScheme();
                }
                return Uni.createFrom().item(new ChallengeData(HttpResponseStatus.UNAUTHORIZED.code(),
                        HttpHeaderNames.WWW_AUTHENTICATE, wwwAuthHeaderValue));
            }
        });
    }
}
