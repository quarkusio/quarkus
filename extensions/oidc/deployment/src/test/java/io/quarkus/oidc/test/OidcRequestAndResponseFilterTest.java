package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hamcrest.Matchers;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.AuthorizationCodeFlow;
import io.quarkus.oidc.BearerTokenAuthentication;
import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.restassured.RestAssured;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class OidcRequestAndResponseFilterTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BearerAuthAndCodeFlowResource.class, BearerTokenRequestFilter.class,
                            BearerTokenResponseFilter.class, AuthorizationCodeFlowResponseFilter.class,
                            AuthorizationCodeFlowRequestFilter.class, FilterCalled.class, CallableFilterParent.class,
                            TenantFeatureBearerTokenResponseFilter.class, TenantFeatureBearerTokenRequestFilter.class,
                            TenantFeatureAuthorizationCodeFlowResponseFilter.class,
                            TenantFeatureAuthorizationCodeFlowRequestFilter.class,
                            RequestTenantFeatureAuthorizationCodeFlowRequestFilter.class,
                            ResponseTenantFeatureAuthorizationCodeFlowResponseFilter.class,
                            RequestTenantFeatureBearerTokenRequestFilter.class, RequestAndResponseFilter.class,
                            ResponseTenantFeatureBearerTokenResponseFilter.class)
                    .addAsResource(
                            new StringAsset(
                                    """
                                            quarkus.oidc.tenant-two.auth-server-url=${keycloak.url}/realms/quarkus
                                            quarkus.oidc.tenant-two.client-id=quarkus-service-app
                                            quarkus.oidc.tenant-two.credentials.secret=secret
                                            quarkus.oidc.tenant-two.authentication.user-info-required=true
                                            quarkus.oidc.tenant-two.tenant-paths=/bearer-and-code-flow/tenant-two*
                                            quarkus.oidc.tenant-one.auth-server-url=${keycloak.url}/realms/quarkus
                                            quarkus.oidc.tenant-one.client-id=quarkus-service-app
                                            quarkus.oidc.tenant-one.credentials.secret=secret
                                            quarkus.oidc.tenant-one.authentication.user-info-required=true
                                            quarkus.oidc.tenant-one.tenant-paths=/bearer-and-code-flow/tenant-one*
                                            quarkus.oidc.code-tenant.auth-server-url=${keycloak.url}/realms/quarkus
                                            quarkus.oidc.code-tenant.client-id=quarkus-web-app
                                            quarkus.oidc.code-tenant.credentials.secret=secret
                                            quarkus.oidc.code-tenant.application-type=web-app
                                            quarkus.oidc.code-tenant.tenant-paths=/bearer-and-code-flow/code-flow*
                                            quarkus.oidc.bearer-tenant.auth-server-url=${keycloak.url}/realms/quarkus
                                            quarkus.oidc.bearer-tenant.client-id=quarkus-service-app
                                            quarkus.oidc.bearer-tenant.credentials.secret=secret
                                            quarkus.oidc.bearer-tenant.authentication.user-info-required=true
                                            quarkus.oidc.bearer-tenant.tenant-paths=/bearer-and-code-flow/bearer*
                                            quarkus.oidc.my-code-tenant.auth-server-url=${keycloak.url}/realms/quarkus
                                            quarkus.oidc.my-code-tenant.client-id=quarkus-web-app
                                            quarkus.oidc.my-code-tenant.credentials.secret=secret
                                            quarkus.oidc.my-code-tenant.application-type=web-app
                                            quarkus.oidc.my-code-tenant.tenant-paths=/bearer-and-code-flow/tenant-feature-code-flow*
                                            quarkus.oidc.my-bearer-tenant.auth-server-url=${keycloak.url}/realms/quarkus
                                            quarkus.oidc.my-bearer-tenant.client-id=quarkus-service-app
                                            quarkus.oidc.my-bearer-tenant.credentials.secret=secret
                                            quarkus.oidc.my-bearer-tenant.authentication.user-info-required=true
                                            quarkus.oidc.my-bearer-tenant.tenant-paths=/bearer-and-code-flow/tenant-feature-bearer*
                                            quarkus.oidc.request-my-code-tenant.auth-server-url=${keycloak.url}/realms/quarkus
                                            quarkus.oidc.request-my-code-tenant.client-id=quarkus-web-app
                                            quarkus.oidc.request-my-code-tenant.credentials.secret=secret
                                            quarkus.oidc.request-my-code-tenant.application-type=web-app
                                            quarkus.oidc.request-my-code-tenant.tenant-paths=/bearer-and-code-flow/request-tenant-feature-code-flow*
                                            quarkus.oidc.request-my-bearer-tenant.auth-server-url=${keycloak.url}/realms/quarkus
                                            quarkus.oidc.request-my-bearer-tenant.client-id=quarkus-service-app
                                            quarkus.oidc.request-my-bearer-tenant.credentials.secret=secret
                                            quarkus.oidc.request-my-bearer-tenant.authentication.user-info-required=true
                                            quarkus.oidc.request-my-bearer-tenant.tenant-paths=/bearer-and-code-flow/request-tenant-feature-bearer*
                                            quarkus.oidc.response-my-code-tenant.auth-server-url=${keycloak.url}/realms/quarkus
                                            quarkus.oidc.response-my-code-tenant.client-id=quarkus-web-app
                                            quarkus.oidc.response-my-code-tenant.credentials.secret=secret
                                            quarkus.oidc.response-my-code-tenant.application-type=web-app
                                            quarkus.oidc.response-my-code-tenant.tenant-paths=/bearer-and-code-flow/response-tenant-feature-code-flow*
                                            quarkus.oidc.response-my-bearer-tenant.auth-server-url=${keycloak.url}/realms/quarkus
                                            quarkus.oidc.response-my-bearer-tenant.client-id=quarkus-service-app
                                            quarkus.oidc.response-my-bearer-tenant.credentials.secret=secret
                                            quarkus.oidc.response-my-bearer-tenant.authentication.user-info-required=true
                                            quarkus.oidc.response-my-bearer-tenant.tenant-paths=/bearer-and-code-flow/response-tenant-feature-bearer*
                                            quarkus.log.category."org.htmlunit.javascript.host.css.CSSStyleSheet".level=FATAL
                                            quarkus.log.category."org.htmlunit.css".level=FATAL
                                            """),
                            "application.properties"));

    @Test
    public void testTwoTenantsAndJointRequestAndResponseFilter() {
        // class RequestAndResponseFilter is annotated with @TenantFeature({ "tenant-one", "tenant-two" })
        // and implements both OIDC request and response filter
        resetFilterState();
        RestAssured.given().auth().oauth2(getAccessToken()).get("/bearer-and-code-flow/tenant-two")
                .then().statusCode(200).body(Matchers.is("alice"));
        var filterCalled = getFilterCalled(); // also resets state
        assertTrue(filterCalled.bearerRequest());
        assertTrue(filterCalled.bearerResponse());
        assertFalse(filterCalled.codeRequest());
        assertFalse(filterCalled.codeResponse());
        assertFalse(filterCalled.tenantFeatureBearerRequest());
        assertFalse(filterCalled.tenantFeatureBearerResponse());
        assertFalse(filterCalled.tenantFeatureCodeRequest());
        assertFalse(filterCalled.tenantFeatureCodeResponse());
        assertFalse(filterCalled.requestTenantFeatureBearerRequest());
        assertFalse(filterCalled.requestTenantFeatureCodeRequest());
        assertFalse(filterCalled.responseTenantFeatureBearerResponse());
        assertFalse(filterCalled.responseTenantFeatureCodeResponse());
        assertTrue(filterCalled.requestAndResponseFilter());
        RestAssured.given().auth().oauth2(getAccessToken()).get("/bearer-and-code-flow/tenant-one")
                .then().statusCode(200).body(Matchers.is("alice"));
        filterCalled = getFilterCalled();
        assertTrue(filterCalled.bearerRequest());
        assertTrue(filterCalled.bearerResponse());
        assertFalse(filterCalled.codeRequest());
        assertFalse(filterCalled.codeResponse());
        assertFalse(filterCalled.tenantFeatureBearerRequest());
        assertFalse(filterCalled.tenantFeatureBearerResponse());
        assertFalse(filterCalled.tenantFeatureCodeRequest());
        assertFalse(filterCalled.tenantFeatureCodeResponse());
        assertFalse(filterCalled.requestTenantFeatureBearerRequest());
        assertFalse(filterCalled.requestTenantFeatureCodeRequest());
        assertFalse(filterCalled.responseTenantFeatureBearerResponse());
        assertFalse(filterCalled.responseTenantFeatureCodeResponse());
        assertTrue(filterCalled.requestAndResponseFilter());
    }

    @Test
    public void testTenantFeatureSelectingBearerAuthTenantFilter_ResponseFilter() {
        resetFilterState();
        RestAssured.given().auth().oauth2(getAccessToken()).get("/bearer-and-code-flow/response-tenant-feature-bearer")
                .then().statusCode(200).body(Matchers.is("alice"));
        var filterCalled = getFilterCalled();
        assertTrue(filterCalled.bearerRequest());
        assertTrue(filterCalled.bearerResponse());
        assertFalse(filterCalled.codeRequest());
        assertFalse(filterCalled.codeResponse());
        assertFalse(filterCalled.tenantFeatureBearerRequest());
        assertFalse(filterCalled.tenantFeatureBearerResponse());
        assertFalse(filterCalled.tenantFeatureCodeRequest());
        assertFalse(filterCalled.tenantFeatureCodeResponse());
        assertFalse(filterCalled.requestTenantFeatureBearerRequest());
        assertFalse(filterCalled.requestTenantFeatureCodeRequest());
        assertTrue(filterCalled.responseTenantFeatureBearerResponse());
        assertFalse(filterCalled.responseTenantFeatureCodeResponse());
        assertFalse(filterCalled.requestAndResponseFilter());
    }

    @Test
    public void testTenantFeatureSelectingCodeFlowTenant_ResponseFilter() throws IOException {
        resetFilterState();
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8080/bearer-and-code-flow/response-tenant-feature-code-flow");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("alice", page.getBody().asNormalizedText());

            webClient.getCookieManager().clearCookies();

            var filterCalled = getFilterCalled();
            assertFalse(filterCalled.bearerRequest());
            assertFalse(filterCalled.bearerResponse());
            assertTrue(filterCalled.codeRequest());
            assertTrue(filterCalled.codeResponse());
            assertFalse(filterCalled.tenantFeatureBearerRequest());
            assertFalse(filterCalled.tenantFeatureBearerResponse());
            assertFalse(filterCalled.tenantFeatureCodeRequest());
            assertFalse(filterCalled.tenantFeatureCodeResponse());
            assertFalse(filterCalled.requestTenantFeatureBearerRequest());
            assertFalse(filterCalled.requestTenantFeatureCodeRequest());
            assertFalse(filterCalled.responseTenantFeatureBearerResponse());
            assertTrue(filterCalled.responseTenantFeatureCodeResponse());
            assertFalse(filterCalled.requestAndResponseFilter());
        }
    }

    @Test
    public void testTenantFeatureSelectingBearerAuthTenantFilter_RequestFilter() {
        resetFilterState();
        RestAssured.given().auth().oauth2(getAccessToken()).get("/bearer-and-code-flow/request-tenant-feature-bearer")
                .then().statusCode(200).body(Matchers.is("alice"));
        var filterCalled = getFilterCalled();
        assertTrue(filterCalled.bearerRequest());
        assertTrue(filterCalled.bearerResponse());
        assertFalse(filterCalled.codeRequest());
        assertFalse(filterCalled.codeResponse());
        assertFalse(filterCalled.tenantFeatureBearerRequest());
        assertFalse(filterCalled.tenantFeatureBearerResponse());
        assertFalse(filterCalled.tenantFeatureCodeRequest());
        assertFalse(filterCalled.tenantFeatureCodeResponse());
        assertTrue(filterCalled.requestTenantFeatureBearerRequest());
        assertFalse(filterCalled.requestTenantFeatureCodeRequest());
        assertFalse(filterCalled.responseTenantFeatureBearerResponse());
        assertFalse(filterCalled.responseTenantFeatureCodeResponse());
        assertFalse(filterCalled.requestAndResponseFilter());
    }

    @Test
    public void testTenantFeatureSelectingCodeFlowTenant_RequestFilter() throws IOException {
        resetFilterState();
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8080/bearer-and-code-flow/request-tenant-feature-code-flow");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("alice", page.getBody().asNormalizedText());

            webClient.getCookieManager().clearCookies();

            var filterCalled = getFilterCalled();
            assertFalse(filterCalled.bearerRequest());
            assertFalse(filterCalled.bearerResponse());
            assertTrue(filterCalled.codeRequest());
            assertTrue(filterCalled.codeResponse());
            assertFalse(filterCalled.tenantFeatureBearerRequest());
            assertFalse(filterCalled.tenantFeatureBearerResponse());
            assertFalse(filterCalled.tenantFeatureCodeRequest());
            assertFalse(filterCalled.tenantFeatureCodeResponse());
            assertFalse(filterCalled.requestTenantFeatureBearerRequest());
            assertTrue(filterCalled.requestTenantFeatureCodeRequest());
            assertFalse(filterCalled.responseTenantFeatureBearerResponse());
            assertFalse(filterCalled.responseTenantFeatureCodeResponse());
            assertFalse(filterCalled.requestAndResponseFilter());
        }
    }

    @Test
    public void testTenantFeatureSelectingBearerAuthTenantFilter_ResponseAndRequestFilter() {
        resetFilterState();
        RestAssured.given().auth().oauth2(getAccessToken()).get("/bearer-and-code-flow/tenant-feature-bearer")
                .then().statusCode(200).body(Matchers.is("alice"));
        var filterCalled = getFilterCalled();
        assertTrue(filterCalled.bearerRequest());
        assertTrue(filterCalled.bearerResponse());
        assertFalse(filterCalled.codeRequest());
        assertFalse(filterCalled.codeResponse());
        assertTrue(filterCalled.tenantFeatureBearerRequest());
        assertTrue(filterCalled.tenantFeatureBearerResponse());
        assertFalse(filterCalled.tenantFeatureCodeRequest());
        assertFalse(filterCalled.tenantFeatureCodeResponse());
        assertFalse(filterCalled.requestTenantFeatureBearerRequest());
        assertFalse(filterCalled.requestTenantFeatureCodeRequest());
        assertFalse(filterCalled.responseTenantFeatureBearerResponse());
        assertFalse(filterCalled.responseTenantFeatureCodeResponse());
        assertFalse(filterCalled.requestAndResponseFilter());
    }

    @Test
    public void testTenantFeatureSelectingCodeFlowTenant_ResponseAndRequestFilter() throws IOException {
        resetFilterState();
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8080/bearer-and-code-flow/tenant-feature-code-flow");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("alice", page.getBody().asNormalizedText());

            webClient.getCookieManager().clearCookies();

            var filterCalled = getFilterCalled();
            assertFalse(filterCalled.bearerRequest());
            assertFalse(filterCalled.bearerResponse());
            assertTrue(filterCalled.codeRequest());
            assertTrue(filterCalled.codeResponse());
            assertFalse(filterCalled.tenantFeatureBearerRequest());
            assertFalse(filterCalled.tenantFeatureBearerResponse());
            assertTrue(filterCalled.tenantFeatureCodeRequest());
            assertTrue(filterCalled.tenantFeatureCodeResponse());
            assertFalse(filterCalled.requestTenantFeatureBearerRequest());
            assertFalse(filterCalled.requestTenantFeatureCodeRequest());
            assertFalse(filterCalled.responseTenantFeatureBearerResponse());
            assertFalse(filterCalled.responseTenantFeatureCodeResponse());
            assertFalse(filterCalled.requestAndResponseFilter());
        }
    }

    @Test
    public void testBearerTokenAuthenticationFilter() {
        resetFilterState();
        RestAssured.given().auth().oauth2(getAccessToken()).get("/bearer-and-code-flow/bearer")
                .then().statusCode(200).body(Matchers.is("alice"));
        var filterCalled = getFilterCalled();
        assertTrue(filterCalled.bearerRequest());
        assertTrue(filterCalled.bearerResponse());
        assertFalse(filterCalled.codeRequest());
        assertFalse(filterCalled.codeResponse());
        assertFalse(filterCalled.tenantFeatureBearerRequest());
        assertFalse(filterCalled.tenantFeatureBearerResponse());
        assertFalse(filterCalled.tenantFeatureCodeRequest());
        assertFalse(filterCalled.tenantFeatureCodeResponse());
        assertFalse(filterCalled.requestTenantFeatureBearerRequest());
        assertFalse(filterCalled.requestTenantFeatureCodeRequest());
        assertFalse(filterCalled.responseTenantFeatureBearerResponse());
        assertFalse(filterCalled.responseTenantFeatureCodeResponse());
        assertFalse(filterCalled.requestAndResponseFilter());
    }

    @Test
    public void testCodeFlowFilter() throws IOException {
        resetFilterState();
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8080/bearer-and-code-flow/code-flow");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("alice", page.getBody().asNormalizedText());

            webClient.getCookieManager().clearCookies();

            var filterCalled = getFilterCalled();
            assertFalse(filterCalled.bearerRequest());
            assertFalse(filterCalled.bearerResponse());
            assertTrue(filterCalled.codeRequest());
            assertTrue(filterCalled.codeResponse());
            assertFalse(filterCalled.tenantFeatureBearerRequest());
            assertFalse(filterCalled.tenantFeatureBearerResponse());
            assertFalse(filterCalled.tenantFeatureCodeRequest());
            assertFalse(filterCalled.tenantFeatureCodeResponse());
            assertFalse(filterCalled.requestTenantFeatureBearerRequest());
            assertFalse(filterCalled.requestTenantFeatureCodeRequest());
            assertFalse(filterCalled.responseTenantFeatureBearerResponse());
            assertFalse(filterCalled.responseTenantFeatureCodeResponse());
            assertFalse(filterCalled.requestAndResponseFilter());
        }
    }

    private static void resetFilterState() {
        getFilterCalled();
    }

    private static FilterCalled getFilterCalled() {
        return RestAssured.given().get("/bearer-and-code-flow/filter-called").then().statusCode(200).extract().body()
                .as(FilterCalled.class);
    }

    private static WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private static String getAccessToken() {
        return KeycloakTestResourceLifecycleManager.getAccessToken("alice");
    }

    @Path("bearer-and-code-flow")
    public static class BearerAuthAndCodeFlowResource {

        @Inject
        @IdToken
        JsonWebToken idToken;

        @Inject
        JsonWebToken accessToken;

        @Inject
        AuthorizationCodeFlowRequestFilter codeRequestFilter;

        @Inject
        AuthorizationCodeFlowResponseFilter codeResponseFilter;

        @Inject
        BearerTokenRequestFilter bearerRequestFilter;

        @Inject
        BearerTokenResponseFilter bearerResponseFilter;

        @Any
        @Inject
        TenantFeatureAuthorizationCodeFlowRequestFilter tenantFeatureCodeRequestFilter;

        @Any
        @Inject
        TenantFeatureAuthorizationCodeFlowResponseFilter tenantFeatureCodeResponseFilter;

        @Any
        @Inject
        TenantFeatureBearerTokenRequestFilter tenantFeatureBearerRequestFilter;

        @Any
        @Inject
        TenantFeatureBearerTokenResponseFilter tenantFeatureBearerResponseFilter;

        @Any
        @Inject
        RequestTenantFeatureAuthorizationCodeFlowRequestFilter requestTenantFeatureAuthorizationCodeRequestFilter;

        @Any
        @Inject
        ResponseTenantFeatureAuthorizationCodeFlowResponseFilter responseTenantFeatureAuthorizationCodeFlowResponseFilter;

        @Any
        @Inject
        RequestTenantFeatureBearerTokenRequestFilter requestTenantFeatureBearerTokenRequestFilter;

        @Any
        @Inject
        ResponseTenantFeatureBearerTokenResponseFilter responseTenantFeatureBearerTokenResponseFilter;

        @Any
        @Inject
        RequestAndResponseFilter requestAndResponseFilter;

        @Authenticated
        @GET
        @Path("tenant-two")
        public String tenantTwo() {
            return accessToken.getName();
        }

        @Authenticated
        @GET
        @Path("tenant-one")
        public String tenantOne() {
            return accessToken.getName();
        }

        @Authenticated
        @GET
        @Path("bearer")
        public String bearer() {
            return accessToken.getName();
        }

        @Authenticated
        @GET
        @Path("code-flow")
        public String codeFlow() {
            return idToken.getName();
        }

        @Authenticated
        @GET
        @Path("tenant-feature-bearer")
        public String tenantFeatureBearer() {
            return accessToken.getName();
        }

        @Authenticated
        @GET
        @Path("tenant-feature-code-flow")
        public String tenantFeatureCodeFlow() {
            return idToken.getName();
        }

        @Authenticated
        @GET
        @Path("request-tenant-feature-bearer")
        public String requestTenantFeatureBearer() {
            return accessToken.getName();
        }

        @Authenticated
        @GET
        @Path("request-tenant-feature-code-flow")
        public String requestTenantFeatureCodeFlow() {
            return idToken.getName();
        }

        @Authenticated
        @GET
        @Path("response-tenant-feature-bearer")
        public String responseTenantFeatureBearer() {
            return accessToken.getName();
        }

        @Authenticated
        @GET
        @Path("response-tenant-feature-code-flow")
        public String responseTenantFeatureCodeFlow() {
            return idToken.getName();
        }

        @Path("filter-called")
        @GET
        public FilterCalled getFilterCalled() {
            var filterCalled = new FilterCalled(bearerRequestFilter.isCalled(), bearerResponseFilter.isCalled(),
                    codeRequestFilter.isCalled(), codeResponseFilter.isCalled(), tenantFeatureBearerRequestFilter.isCalled(),
                    tenantFeatureBearerResponseFilter.isCalled(), tenantFeatureCodeRequestFilter.isCalled(),
                    tenantFeatureCodeResponseFilter.isCalled(), requestTenantFeatureBearerTokenRequestFilter.isCalled(),
                    responseTenantFeatureBearerTokenResponseFilter.isCalled(),
                    requestTenantFeatureAuthorizationCodeRequestFilter.isCalled(),
                    responseTenantFeatureAuthorizationCodeFlowResponseFilter.isCalled(), requestAndResponseFilter.isCalled());
            bearerRequestFilter.reset();
            bearerResponseFilter.reset();
            codeRequestFilter.reset();
            codeResponseFilter.reset();
            tenantFeatureCodeRequestFilter.reset();
            tenantFeatureCodeResponseFilter.reset();
            tenantFeatureBearerRequestFilter.reset();
            tenantFeatureBearerResponseFilter.reset();
            requestTenantFeatureAuthorizationCodeRequestFilter.reset();
            responseTenantFeatureAuthorizationCodeFlowResponseFilter.reset();
            requestTenantFeatureBearerTokenRequestFilter.reset();
            responseTenantFeatureBearerTokenResponseFilter.reset();
            requestAndResponseFilter.reset();
            return filterCalled;
        }
    }

    @BearerTokenAuthentication
    @ApplicationScoped
    public static class BearerTokenResponseFilter extends CallableFilterParent implements OidcResponseFilter {

        @Override
        public void filter(OidcResponseContext responseContext) {
            called();
        }
    }

    @BearerTokenAuthentication
    @ApplicationScoped
    public static class BearerTokenRequestFilter extends CallableFilterParent implements OidcRequestFilter {

        @Override
        public void filter(OidcRequestContext requestContext) {
            called();
        }
    }

    @AuthorizationCodeFlow
    @ApplicationScoped
    public static class AuthorizationCodeFlowResponseFilter extends CallableFilterParent implements OidcResponseFilter {

        @Override
        public void filter(OidcResponseContext responseContext) {
            called();
        }
    }

    @AuthorizationCodeFlow
    @ApplicationScoped
    public static class AuthorizationCodeFlowRequestFilter extends CallableFilterParent implements OidcRequestFilter {

        @Override
        public void filter(OidcRequestContext requestContext) {
            called();
        }

    }

    public static class CallableFilterParent {

        private volatile boolean called = false;

        protected void called() {
            this.called = true;
        }

        public boolean isCalled() {
            return this.called;
        }

        public void reset() {
            this.called = false;
        }
    }

    public record FilterCalled(boolean bearerRequest, boolean bearerResponse, boolean codeRequest, boolean codeResponse,
            boolean tenantFeatureBearerRequest, boolean tenantFeatureBearerResponse,
            boolean tenantFeatureCodeRequest, boolean tenantFeatureCodeResponse,
            boolean requestTenantFeatureBearerRequest, boolean responseTenantFeatureBearerResponse,
            boolean requestTenantFeatureCodeRequest, boolean responseTenantFeatureCodeResponse,
            boolean requestAndResponseFilter) {

    }

    @TenantFeature("my-bearer-tenant")
    @ApplicationScoped
    public static class TenantFeatureBearerTokenResponseFilter extends CallableFilterParent implements OidcResponseFilter {

        @Override
        public void filter(OidcResponseContext responseContext) {
            called();
        }
    }

    @TenantFeature("my-bearer-tenant")
    @ApplicationScoped
    public static class TenantFeatureBearerTokenRequestFilter extends CallableFilterParent implements OidcRequestFilter {

        @Override
        public void filter(OidcRequestContext requestContext) {
            called();
        }
    }

    @TenantFeature("my-code-tenant")
    @ApplicationScoped
    public static class TenantFeatureAuthorizationCodeFlowResponseFilter extends CallableFilterParent
            implements OidcResponseFilter {

        @Override
        public void filter(OidcResponseContext responseContext) {
            called();
        }
    }

    @TenantFeature("my-code-tenant")
    @ApplicationScoped
    public static class TenantFeatureAuthorizationCodeFlowRequestFilter extends CallableFilterParent
            implements OidcRequestFilter {

        @Override
        public void filter(OidcRequestContext requestContext) {
            called();
        }

    }

    @TenantFeature("response-my-bearer-tenant")
    @ApplicationScoped
    public static class ResponseTenantFeatureBearerTokenResponseFilter extends CallableFilterParent
            implements OidcResponseFilter {

        @Override
        public void filter(OidcResponseContext responseContext) {
            called();
        }
    }

    @TenantFeature("request-my-bearer-tenant")
    @ApplicationScoped
    public static class RequestTenantFeatureBearerTokenRequestFilter extends CallableFilterParent implements OidcRequestFilter {

        @Override
        public void filter(OidcRequestContext requestContext) {
            called();
        }
    }

    @TenantFeature("response-my-code-tenant")
    @ApplicationScoped
    public static class ResponseTenantFeatureAuthorizationCodeFlowResponseFilter extends CallableFilterParent
            implements OidcResponseFilter {

        @Override
        public void filter(OidcResponseContext responseContext) {
            called();
        }
    }

    @TenantFeature("request-my-code-tenant")
    @ApplicationScoped
    public static class RequestTenantFeatureAuthorizationCodeFlowRequestFilter extends CallableFilterParent
            implements OidcRequestFilter {

        @Override
        public void filter(OidcRequestContext requestContext) {
            called();
        }

    }

    @TenantFeature({ "tenant-one", "tenant-two" })
    @Singleton
    public static class RequestAndResponseFilter implements OidcResponseFilter, OidcRequestFilter {

        private final CallableFilterParent request = new CallableFilterParent();
        private final CallableFilterParent response = new CallableFilterParent();

        @Override
        public void filter(OidcResponseContext responseContext) {
            response.called();
        }

        @Override
        public void filter(OidcRequestContext requestContext) {
            request.called();
        }

        public void reset() {
            request.reset();
            response.reset();
        }

        public boolean isCalled() {
            return request.isCalled() && response.isCalled();
        }
    }
}
