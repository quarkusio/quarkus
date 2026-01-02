package io.quarkus.vertx.http.security;

import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.event.Observes;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.FormAuthConfig.CookieSameSite;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

class FormAuthRedirectQueryParamsTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot(jar -> jar
            .addClasses(TestIdentityProvider.class, TestIdentityController.class, TestTrustedIdentityProvider.class,
                    PathHandler.class, HttpSecurityConfigurator.class, DelegatingMechanism.class)
            .addAsResource(new StringAsset("""
                    quarkus.http.auth.form.enabled=true
                    quarkus.http.auth.form.login-page-query-params=jfwid
                    quarkus.http.auth.form.landing-page=landing
                    quarkus.http.auth.form.error-page=error
                    quarkus.http.auth.form.login-page=login
                    quarkus.http.auth.form.cookie-same-site=lax
                    quarkus.http.auth.form.cookie-name=laitnederc-sukrauq
                    quarkus.http.auth.form.http-only-cookie=true
                    quarkus.http.auth.form.cookie-max-age=2m
                    """), "application.properties"));

    @BeforeAll
    static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void testSingleQueryParamPassed() {
        Map<String, Set<String>> allQueryParams = Map.of("jfwid", Set.of("1234"));
        Set<String> passedQueryParams = Set.of("jfwid=1234");
        Set<String> ignoredQueryParams = Set.of();
        testLoginPageQueryParams("/admin", allQueryParams, passedQueryParams, ignoredQueryParams, "/login");
        testLandingPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/j_security_check", "/landing");
        testErrorPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/j_security_check", "/error");
        allQueryParams = Map.of("jfwid", Set.of("1234"), "ignored-1", Set.of("haha"));
        passedQueryParams = Set.of("jfwid=1234");
        ignoredQueryParams = Set.of("ignored-1");
        testLoginPageQueryParams("/admin", allQueryParams, passedQueryParams, ignoredQueryParams, "/login");
        testLandingPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/j_security_check", "/landing");
        testErrorPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/j_security_check", "/error");
        allQueryParams = Map.of("whatever", Set.of("9"), "jfwid", Set.of("1234"), "ignored-1", Set.of("haha"));
        passedQueryParams = Set.of("jfwid=1234");
        ignoredQueryParams = Set.of("ignored-1", "whatever");
        testLoginPageQueryParams("/admin", allQueryParams, passedQueryParams, ignoredQueryParams, "/login");
        testLandingPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/j_security_check", "/landing");
        testErrorPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/j_security_check", "/error");
        allQueryParams = Map.of("whatever", Set.of("9"), "unknown", Set.of("1234"), "ignored-1", Set.of("❤"));
        passedQueryParams = Set.of();
        ignoredQueryParams = Set.of("ignored-1", "whatever", "unknown");
        testLoginPageQueryParams("/admin", allQueryParams, passedQueryParams, ignoredQueryParams, "/login");
        testLandingPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/j_security_check", "/landing");
        testErrorPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/j_security_check", "/error");
        allQueryParams = Map.of();
        passedQueryParams = Set.of();
        ignoredQueryParams = Set.of();
        testLoginPageQueryParams("/admin", allQueryParams, passedQueryParams, ignoredQueryParams, "/login");
        testLandingPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/j_security_check", "/landing");
        testErrorPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/j_security_check", "/error");
    }

    @Test
    void testMultipleQueryParamsPassed() {
        // following parameters are set for all the parameters
        Map<String, Set<String>> allQueryParams = Map.of("elvis", Set.of("Presley"), "jfwid", Set.of("1234"), "jitka",
                Set.of("teacher"), "otherid", Set.of("8765"), "mavis", Set.of("staples"));
        Set<String> passedQueryParams = Set.of("jfwid=1234");
        Set<String> ignoredQueryParams = Set.of("jitka", "mavis", "elvis");
        testLoginPageQueryParams("/multiple-params", allQueryParams, passedQueryParams, ignoredQueryParams, "/m-login");
        testLandingPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/m_security_check", "/m-landing");
        testErrorPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/m_security_check", "/m-error");

        // the 'login-only' pass-through parameter is only set for the login page
        allQueryParams = Map.of("elvis", Set.of("Presley"), "login-only", Set.of("8765"), "mavis", Set.of("staples"));
        passedQueryParams = Set.of("login-only=8765");
        ignoredQueryParams = Set.of("mavis", "elvis");
        testLoginPageQueryParams("/multiple-params", allQueryParams, passedQueryParams, ignoredQueryParams, "/m-login");
        passedQueryParams = Set.of();
        ignoredQueryParams = Set.of("mavis", "elvis", "login-only");
        testLandingPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/m_security_check", "/m-landing");
        testErrorPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/m_security_check", "/m-error");

        // the 'landing-only' pass-through parameter is only set for the login page
        allQueryParams = Map.of("elvis", Set.of("Presley"), "landing-only", Set.of("8765"), "mavis", Set.of("staples"));
        passedQueryParams = Set.of("landing-only=8765");
        ignoredQueryParams = Set.of("mavis", "elvis");
        testLandingPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/m_security_check", "/m-landing");
        passedQueryParams = Set.of();
        ignoredQueryParams = Set.of("mavis", "elvis", "landing-only");
        testLoginPageQueryParams("/multiple-params", allQueryParams, passedQueryParams, ignoredQueryParams, "/m-login");
        testErrorPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/m_security_check", "/m-error");

        // the 'error-only' pass-through parameter is only set for the login page
        allQueryParams = Map.of("elvis", Set.of("Presley"), "error-only", Set.of("8765"), "mavis", Set.of("staples"));
        passedQueryParams = Set.of("error-only=8765");
        ignoredQueryParams = Set.of("mavis", "elvis");
        testErrorPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/m_security_check", "/m-error");
        passedQueryParams = Set.of();
        ignoredQueryParams = Set.of("mavis", "elvis", "error-only");
        testLoginPageQueryParams("/multiple-params", allQueryParams, passedQueryParams, ignoredQueryParams, "/m-login");
        testLandingPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/m_security_check", "/m-landing");
    }

    @Test
    void testMultipleQueryParamValues() {
        Map<String, Set<String>> allQueryParams = Map.of("elvis", Set.of("Presley"), "jfwid",
                Set.of("1234", "2345", "3456", "4567"),
                "jitka", Set.of("teacher"), "otherid", Set.of("8765"), "mavis", Set.of("staples"));
        Set<String> passedQueryParams = Set.of("jfwid=1234", "jfwid=2345", "jfwid=3456", "jfwid=4567");
        Set<String> ignoredQueryParams = Set.of("jitka", "mavis", "elvis");
        testLoginPageQueryParams("/multiple-params", allQueryParams, passedQueryParams, ignoredQueryParams, "/m-login");
        testLandingPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/m_security_check", "/m-landing");
        testErrorPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/m_security_check", "/m-error");
    }

    @Test
    void testQueryParamEncoding() {
        Map<String, Set<String>> allQueryParams = Map.of("jfwid", Set.of("Test%20Case & Check", "a=b&c=d", "/path?q=1"));
        // we expect that the query param value was encoded, so that we stay safe; the client can decode it if necessary
        Set<String> passedQueryParams = Set.of("jfwid=Test%2520Case%20%26%20Check", "jfwid=a%3Db%26c%3Dd",
                "jfwid=%2Fpath%3Fq%3D1");
        Set<String> ignoredQueryParams = Set.of();
        testLoginPageQueryParams("/admin", allQueryParams, passedQueryParams, ignoredQueryParams, "/login");
        testLandingPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/j_security_check", "/landing");
        testErrorPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/j_security_check", "/error");
    }

    @Test
    void testRedirectLocationWithQuery() {
        Map<String, Set<String>> allQueryParams = Map.of("elvis", Set.of("Presley"), "jfwid",
                Set.of("1234", "2345", "3456", "4567"),
                "jitka", Set.of("teacher"), "otherid", Set.of("8765"), "mavis", Set.of("staples"));
        Set<String> passedQueryParams = Set.of("jfwid=1234", "jfwid=2345", "jfwid=3456", "jfwid=4567");
        Set<String> ignoredQueryParams = Set.of("jitka", "mavis", "elvis");
        testLoginPageQueryParams("/redirect-location-with-query", allQueryParams, passedQueryParams, ignoredQueryParams,
                "/log-me-in?failed=that-is-life");
        testLandingPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/q_security_check",
                "/landing?avalon");
        testErrorPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/q_security_check",
                "/error?what-a-surprise=ack");
    }

    @Test
    void testNoQueryParamPassed() {
        Map<String, Set<String>> allQueryParams = Map.of("jfwid", Set.of("1234"));
        Set<String> passedQueryParams = Set.of();
        Set<String> ignoredQueryParams = Set.of("jfwid");
        testLoginPageQueryParams("/no-params", allQueryParams, passedQueryParams, ignoredQueryParams, "/n-login");
        testLandingPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/n_security_check", "/n-landing");
        testErrorPageQueryParams(allQueryParams, passedQueryParams, ignoredQueryParams, "/n_security_check", "/n-error");
    }

    private static void testErrorPageQueryParams(Map<String, Set<String>> queryParams, Set<String> passedQueryParams,
            Set<String> ignoredQueryParams, String jSecurityCheckPath, String errorPage) {
        var passedParamsMatcher = getPassedParamsMatcher(passedQueryParams);

        var request = RestAssured.given();
        queryParams.forEach(request::queryParam);
        request
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .formParam("j_password", "wrong")
                .post(jSecurityCheckPath)
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString(errorPage))
                .header("location", passedParamsMatcher)
                .header("location", getIgnoredParamsMatcher(ignoredQueryParams));
    }

    private static void testLandingPageQueryParams(Map<String, Set<String>> queryParams, Set<String> passedQueryParams,
            Set<String> ignoredQueryParams, String jSecurityCheckPath, String landingPage) {
        var passedParamsMatcher = getPassedParamsMatcher(passedQueryParams);

        var request = RestAssured.given();
        queryParams.forEach(request::queryParam);
        request
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .post(jSecurityCheckPath)
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString(landingPage))
                .header("location", passedParamsMatcher)
                .header("location", getIgnoredParamsMatcher(ignoredQueryParams))
                .cookie("laitnederc-sukrauq", detailedCookie().value(notNullValue())
                        .httpOnly(true).sameSite("Lax").maxAge(120));
    }

    private static void testLoginPageQueryParams(String path, Map<String, Set<String>> queryParams,
            Set<String> passedQueryParams,
            Set<String> ignoredQueryParams, String loginPage) {
        CookieFilter cookies = new CookieFilter();
        var passedParamsMatcher = getPassedParamsMatcher(passedQueryParams);
        var request = RestAssured.given();
        queryParams.forEach(request::queryParam);
        request
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get(path)
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString(loginPage))
                .header("location", passedParamsMatcher)
                .header("location", getIgnoredParamsMatcher(ignoredQueryParams))
                .cookie("quarkus-redirect-location",
                        detailedCookie().value(allOf(containsString(path), passedParamsMatcher)).sameSite("Lax"));
    }

    private static Matcher<String> getIgnoredParamsMatcher(Set<String> ignoredQueryParams) {
        return ignoredQueryParams.stream().map(Matchers::containsString).map(Matchers::not).reduce(Matchers::allOf)
                .orElse(Matchers.containsString(""));
    }

    private static Matcher<String> getPassedParamsMatcher(Set<String> passedQueryParams) {
        return passedQueryParams.stream().map(Matchers::containsString).reduce(Matchers::allOf)
                .orElse(Matchers.containsString(""));
    }

    static class HttpSecurityConfigurator {

        void configureFormAuthentication(@Observes HttpSecurity httpSecurity) {
            var multipleQueryParams = new DelegatingMechanism(
                    createFormBuilderBase("m-")
                            .errorPageQueryParameters("jfwid", "otherid", "error-only")
                            .landingPageQueryParameters("jfwid", "otherid", "landing-only")
                            .loginPageQueryParameters("jfwid", "otherid", "login-only")
                            .postLocation("m_security_check").build(),
                    "multiple-query-param");
            var noQueryParams = new DelegatingMechanism(createFormBuilderBase("n-").postLocation("n_security_check")
                    .loginPageQueryParameters()
                    .landingPageQueryParameters()
                    .errorPageQueryParameters()
                    .build(),
                    "no-query-param");
            var redirectLocationWithQuery = new DelegatingMechanism(
                    createFormBuilderBase("")
                            .loginPage("log-me-in?failed=that-is-life")
                            .errorPage("error?what-a-surprise=ack")
                            .landingPage("landing?avalon")
                            .errorPageQueryParameters("jfwid", "otherid", "error-only")
                            .landingPageQueryParameters("jfwid", "otherid", "landing-only")
                            .loginPageQueryParameters("jfwid", "otherid", "login-only")
                            .postLocation("q_security_check").build(),
                    "redirect-location-with-query");
            httpSecurity
                    .mechanism(multipleQueryParams)
                    .mechanism(noQueryParams)
                    .mechanism(redirectLocationWithQuery)
                    .path("/admin").form().roles("admin")
                    .path("/multiple-params").authenticatedWith(multipleQueryParams.name).roles("admin")
                    .path("/no-params").authenticatedWith(noQueryParams.name).roles("admin")
                    .path("/redirect-location-with-query").authenticatedWith(redirectLocationWithQuery.name);
        }

        private static Form.Builder createFormBuilderBase(String prefix) {
            return Form.builder()
                    .loginPage(prefix + "login")
                    .errorPage(prefix + "error")
                    .landingPage(prefix + "landing")
                    .timeout(Duration.ofSeconds(2))
                    .newCookieInterval(Duration.ofSeconds(1))
                    .cookieName("laitnederc-sukrauq")
                    .cookieSameSite(CookieSameSite.LAX)
                    .httpOnlyCookie()
                    .cookieMaxAge(Duration.ofMinutes(2))
                    .encryptionKey("CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT");
        }
    }

    private static final class DelegatingMechanism implements HttpAuthenticationMechanism {

        private final HttpAuthenticationMechanism delegate;
        private final String name;

        private DelegatingMechanism(HttpAuthenticationMechanism httpAuthenticationMechanism, String name) {
            this.delegate = httpAuthenticationMechanism;
            this.name = name;
        }

        @Override
        public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
            return delegate.authenticate(context, identityProviderManager);
        }

        @Override
        public Uni<ChallengeData> getChallenge(RoutingContext context) {
            return delegate.getChallenge(context);
        }

        @Override
        public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
            return delegate.getCredentialTypes();
        }

        @Override
        public Uni<Boolean> sendChallenge(RoutingContext context) {
            return delegate.sendChallenge(context);
        }

        @Override
        public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
            return Uni.createFrom()
                    .item(new HttpCredentialTransport(HttpCredentialTransport.Type.POST, "/j_security_check", name));
        }

        @Override
        public int getPriority() {
            return delegate.getPriority();
        }
    }
}
