package io.quarkus.it.keycloak;

import java.net.URI;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.UUID;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import io.smallrye.jwt.build.JwtSignatureBuilder;
import io.smallrye.jwt.util.KeyUtils;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@Path("/single-page-app")
public class FrontendResource {

    @Inject
    TenantConfigBean oidcTenants;

    @Context
    UriInfo ui;

    private final WebClient client;

    @Inject
    public FrontendResource(Vertx vertx) {
        this.client = WebClient.create(vertx);
    }

    @GET
    @Path("login-jwt")
    public Response loginJwt() {
        return redirect("dpop-jwt", "callback-jwt");
    }

    @GET
    @Path("callback-jwt")
    public Response callbackJwt(@QueryParam("code") String code) throws Exception {
        return callProtectedEndpoint(code, "dpop-jwt", "callback-jwt", "GET", "dpop-jwt", "dpop-jwt", false, false, false);
    }

    @GET
    @Path("login-jwt-wrong-dpop-http-method")
    public Response loginJwtWrongDpopHttpMethod() {
        return redirect("dpop-jwt", "callback-jwt-wrong-dpop-http-method");
    }

    @GET
    @Path("callback-jwt-wrong-dpop-http-method")
    public Response callbackWrongDpopHttpMethod(@QueryParam("code") String code) throws Exception {
        return callProtectedEndpoint(code, "dpop-jwt", "callback-jwt-wrong-dpop-http-method", "POST", "dpop-jwt", "dpop-jwt",
                false, false, false);
    }

    @GET
    @Path("login-jwt-wrong-dpop-http-uri")
    public Response loginJwtWrongDpopHttpUri() {
        return redirect("dpop-jwt", "callback-jwt-wrong-dpop-http-uri");
    }

    @GET
    @Path("callback-jwt-wrong-dpop-http-uri")
    public Response callbackWrongDpopHttpUri(@QueryParam("code") String code) throws Exception {
        return callProtectedEndpoint(code, "dpop-jwt", "callback-jwt-wrong-dpop-http-uri", "GET", "dpop-jwt-wrong-uri",
                "dpop-jwt", false, false, false);
    }

    @GET
    @Path("login-jwt-wrong-dpop-signature")
    public Response loginJwtWrongDpopSignature() {
        return redirect("dpop-jwt", "callback-jwt-wrong-dpop-signature");
    }

    @GET
    @Path("callback-jwt-wrong-dpop-signature")
    public Response callbackWrongDpopSignature(@QueryParam("code") String code) throws Exception {
        return callProtectedEndpoint(code, "dpop-jwt", "callback-jwt-wrong-dpop-signature", "GET", "dpop-jwt",
                "dpop-jwt", true, false, false);
    }

    @GET
    @Path("login-jwt-wrong-dpop-jwk-key")
    public Response loginJwtWrongDpopJwkKey() {
        return redirect("dpop-jwt", "callback-jwt-wrong-dpop-jwk-key");
    }

    @GET
    @Path("callback-jwt-wrong-dpop-jwk-key")
    public Response callbackWrongDpopJwkKey(@QueryParam("code") String code) throws Exception {
        return callProtectedEndpoint(code, "dpop-jwt", "callback-jwt-wrong-dpop-jwk-key", "GET", "dpop-jwt-wrong-uri",
                "dpop-jwt", false, true, false);
    }

    @GET
    @Path("login-jwt-wrong-dpop-token-hash")
    public Response loginJwtWrongDpopTokenHash() {
        return redirect("dpop-jwt", "callback-jwt-wrong-dpop-token-hash");
    }

    @GET
    @Path("callback-jwt-wrong-dpop-token-hash")
    public Response callbackWrongDpopTokenHash(@QueryParam("code") String code) throws Exception {
        return callProtectedEndpoint(code, "dpop-jwt", "callback-jwt-wrong-dpop-token-hash", "GET", "dpop-jwt",
                "dpop-jwt", false, false, true);
    }

    private Response callProtectedEndpoint(String code, String tenantId, String redirectPath, String dPopHttpMethod,
            String dPopEndpointPath, String quarkusEndpointPath,
            boolean wrongDpopSignature,
            boolean wrongDpopJwkKey,
            boolean wrongDpopTokenHash)
            throws Exception {
        String redirectUriParam = ui.getBaseUriBuilder().path("single-page-app").path(redirectPath).build().toString();

        MultiMap grantParams = MultiMap.caseInsensitiveMultiMap();
        grantParams.add(OidcConstants.CLIENT_ID, "backend-service");
        grantParams.add(OidcConstants.GRANT_TYPE, OidcConstants.AUTHORIZATION_CODE);
        grantParams.add(OidcConstants.CODE_FLOW_CODE, code);
        grantParams.add(OidcConstants.CODE_FLOW_REDIRECT_URI, redirectUriParam);

        Buffer encoded = OidcCommonUtils.encodeForm(grantParams);
        HttpRequest<Buffer> requestToKeycloak = client.postAbs(getConfigMetadata(tenantId).getTokenUri());
        requestToKeycloak.putHeader("Content-Type", HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString());

        KeyPair keyPair = KeyUtils.generateKeyPair(2048);
        requestToKeycloak.putHeader("DPoP", createDPopProofForKeycloak(keyPair, tenantId));

        JsonObject grantResponse = requestToKeycloak.sendBufferAndAwait(encoded).bodyAsJsonObject();
        String accessToken = grantResponse.getString("access_token");

        String requestPath = ui.getBaseUriBuilder().path("service").path(quarkusEndpointPath).build().toString();
        HttpRequest<Buffer> requestToQuarkus = client.getAbs(requestPath);
        requestToQuarkus.putHeader("Accept", "text/plain");
        String absoluteDpopEndpointUri = ui.getBaseUriBuilder().path("service").path(dPopEndpointPath).build().toString();
        requestToQuarkus.putHeader("DPoP", createDPopProofForQuarkus(keyPair, accessToken, dPopHttpMethod,
                absoluteDpopEndpointUri, wrongDpopSignature, wrongDpopJwkKey, wrongDpopTokenHash));
        requestToQuarkus.putHeader("Authorization", "DPoP " + accessToken);
        HttpResponse<Buffer> response = requestToQuarkus.sendAndAwait();
        return Response.ok(response.bodyAsString()).build();
    }

    private Response redirect(String tenantId, String callbackPath) {
        StringBuilder codeFlowParams = new StringBuilder();

        // response_type
        codeFlowParams.append(OidcConstants.CODE_FLOW_RESPONSE_TYPE).append("=").append(OidcConstants.CODE_FLOW_CODE);
        // client_id
        codeFlowParams.append("&").append(OidcConstants.CLIENT_ID).append("=").append("backend-service");
        // scope
        codeFlowParams.append("&").append(OidcConstants.TOKEN_SCOPE).append("=").append("openid");
        // redirect_uri
        String redirectUriParam = ui.getBaseUriBuilder().path("single-page-app/" + callbackPath).build().toString();
        codeFlowParams.append("&").append(OidcConstants.CODE_FLOW_REDIRECT_URI).append("=")
                .append(OidcCommonUtils.urlEncode(redirectUriParam));
        // state
        String state = UUID.randomUUID().toString();
        codeFlowParams.append("&").append(OidcConstants.CODE_FLOW_STATE).append("=").append(state);
        return Response
                .seeOther(URI.create(getConfigMetadata(tenantId).getAuthorizationUri() + "?" + codeFlowParams.toString()))
                .cookie(new NewCookie("state", state))
                .build();
    }

    @PreDestroy
    void close() {
        client.close();
    }

    private String createDPopProofForKeycloak(KeyPair keyPair, String tenantId) throws Exception {

        return Jwt.claim("htm", "POST")
                .claim("htu", getConfigMetadata(tenantId).getTokenUri())
                .jws()
                .header("typ", "dpop+jwt")
                .jwk(keyPair.getPublic())
                .sign(keyPair.getPrivate());
    }

    private String createDPopProofForQuarkus(KeyPair keyPair, String accessToken, String dPopHttpMethod,
            String dPopEndpointPath,
            boolean wrongDpopSignature,
            boolean wrongDpopJwkKey,
            boolean wrongAccesstokenHash) throws Exception {

        JwtClaimsBuilder jwtClaimsBuilder = Jwt.claim("htm", dPopHttpMethod)
                .claim("htu", dPopEndpointPath);
        JwtSignatureBuilder jwtSignatureBuilder = jwtClaimsBuilder
                .claim("ath", wrongAccesstokenHash ? accessToken
                        : OidcCommonUtils.base64UrlEncode(
                                OidcUtils.getSha256Digest(accessToken)))
                .jws()
                .header("typ", "dpop+jwt");

        jwtSignatureBuilder = wrongDpopJwkKey ? jwtSignatureBuilder.jwk(KeyUtils.generateKeyPair(2048).getPublic())
                : jwtSignatureBuilder.jwk(keyPair.getPublic());

        PrivateKey signingKey = wrongDpopSignature ? KeyUtils.generateKeyPair(2048).getPrivate()
                : keyPair.getPrivate();
        return jwtSignatureBuilder.sign(signingKey);
    }

    private OidcConfigurationMetadata getConfigMetadata(String tenantId) {
        return oidcTenants.getStaticTenant(tenantId).getOidcMetadata();
    }
}
