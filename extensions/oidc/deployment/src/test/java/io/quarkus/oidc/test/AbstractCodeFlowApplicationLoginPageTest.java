package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.htmlunit.Page;
import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.http.Cookie;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;

abstract class AbstractCodeFlowApplicationLoginPageTest {

    static JavaArchive application(JavaArchive jar, boolean proactiveAuthentication) {
        return jar.addClasses(ApplicationLoginPageResource.class, ApplicationLoginPageRedirectFilter.class)
                .addAsResource(new StringAsset(properties(proactiveAuthentication)), "application.properties");
    }

    private static String properties(boolean proactiveAuthentication) {
        return """
                quarkus.http.auth.proactive=%s
                quarkus.keycloak.devservices.enabled=false
                quarkus.log.category."org.htmlunit".level=ERROR

                quarkus.http.auth.permission.authenticated.paths=/*
                quarkus.http.auth.permission.authenticated.policy=authenticated
                quarkus.http.auth.permission.public.paths=/login,/tenant-restore/login,/tenant-strict/login,/tenant-strict/public
                quarkus.http.auth.permission.public.policy=permit

                quarkus.oidc.auth-server-url=${keycloak.url}/realms/quarkus
                quarkus.oidc.client-id=quarkus-web-app
                quarkus.oidc.credentials.secret=secret
                quarkus.oidc.application-type=web-app
                quarkus.oidc.authentication.application-login-path=/login

                quarkus.oidc.tenant-restore.auth-server-url=${keycloak.url}/realms/quarkus
                quarkus.oidc.tenant-restore.client-id=quarkus-web-app
                quarkus.oidc.tenant-restore.credentials.secret=secret
                quarkus.oidc.tenant-restore.application-type=web-app
                quarkus.oidc.tenant-restore.tenant-paths=/tenant-restore/*
                quarkus.oidc.tenant-restore.authentication.application-login-path=/tenant-restore/login
                quarkus.oidc.tenant-restore.authentication.redirect-path=/tenant-restore/callback
                quarkus.oidc.tenant-restore.authentication.restore-path-after-redirect=true
                quarkus.oidc.tenant-restore.authentication.pkce-required=true

                quarkus.oidc.tenant-strict.auth-server-url=${keycloak.url}/realms/quarkus
                quarkus.oidc.tenant-strict.client-id=quarkus-web-app
                quarkus.oidc.tenant-strict.credentials.secret=secret
                quarkus.oidc.tenant-strict.application-type=web-app
                quarkus.oidc.tenant-strict.tenant-paths=/tenant-strict/*
                quarkus.oidc.tenant-strict.authentication.application-login-path=/tenant-strict/login
                quarkus.oidc.tenant-strict.authentication.allow-multiple-code-flows=false
                quarkus.oidc.tenant-strict.authentication.fail-on-missing-state-param=true
                quarkus.oidc.tenant-strict.authentication.java-script-auto-redirect=false

                quarkus.oidc.tenant-protected-login.auth-server-url=${keycloak.url}/realms/quarkus
                quarkus.oidc.tenant-protected-login.client-id=quarkus-web-app
                quarkus.oidc.tenant-protected-login.credentials.secret=secret
                quarkus.oidc.tenant-protected-login.application-type=web-app
                quarkus.oidc.tenant-protected-login.tenant-paths=/tenant-protected-login/*
                quarkus.oidc.tenant-protected-login.authentication.application-login-path=tenant-protected-login/login
                """
                .formatted(proactiveAuthentication);
    }

    @Test
    public void testLoginPageAndRestoredQuery() throws Exception {
        try (WebClient webClient = createWebClient()) {
            WebResponse challenge = get(webClient, "http://localhost:8081/protected?a=b");
            assertEquals(302, challenge.getStatusCode());
            String loginPageUrl = challenge.getResponseHeaderValue("location");
            assertEquals("http://localhost:8081/login?redirect=%2Fprotected%3Fa%3Db%26q_login%3Dtrue&login-page-filter=applied",
                    loginPageUrl);
            assertFalse(hasCookie(webClient, "q_auth"));
            assertFalse(hasCookie(webClient, "q_session"));

            HtmlPage loginPage = webClient.getPage(loginPageUrl);
            HtmlAnchor link = loginPage.getHtmlElementById("login");
            assertEquals("/protected?a=b&q_login=true", link.getHrefAttribute());

            HtmlPage keycloakPage = link.click();
            assertEquals("Sign in to quarkus", keycloakPage.getTitleText());
            assertTrue(hasCookie(webClient, "q_auth"));
            String keycloakUrl = keycloakPage.getUrl().toString();
            assertTrue(keycloakUrl.contains("redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Fprotected&"), keycloakUrl);
            assertFalse(keycloakUrl.contains("q_login"), keycloakUrl);
            assertFalse(keycloakUrl.contains("login-page-filter"), keycloakUrl);

            Page page = login(keycloakPage);
            assertEquals("http://localhost:8081/protected?a=b", page.getUrl().toString());
            assertEquals("alice:/protected?a=b", page.getWebResponse().getContentAsString());
            assertTrue(hasCookie(webClient, "q_session"));
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testLoginPageWithoutQuery() throws Exception {
        try (WebClient webClient = createWebClient()) {
            WebResponse challenge = get(webClient, "http://localhost:8081/protected");
            assertEquals(302, challenge.getStatusCode());
            String loginPageUrl = challenge.getResponseHeaderValue("location");
            assertEquals("http://localhost:8081/login?redirect=%2Fprotected%3Fq_login%3Dtrue&login-page-filter=applied",
                    loginPageUrl);

            HtmlPage loginPage = webClient.getPage(loginPageUrl);
            HtmlAnchor link = loginPage.getHtmlElementById("login");
            assertEquals("/protected?q_login=true", link.getHrefAttribute());

            Page page = login(link.click());
            assertEquals("http://localhost:8081/protected", page.getUrl().toString());
            assertEquals("alice:/protected", page.getWebResponse().getContentAsString());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testRedirectPathAndRestoredPath() throws Exception {
        try (WebClient webClient = createWebClient()) {
            WebResponse challenge = get(webClient, "http://localhost:8081/tenant-restore/protected?x=1");
            assertEquals(302, challenge.getStatusCode());
            String loginPageUrl = challenge.getResponseHeaderValue("location");
            assertEquals("http://localhost:8081/tenant-restore/login"
                    + "?redirect=%2Ftenant-restore%2Fprotected%3Fx%3D1%26q_login%3Dtrue&login-page-filter=applied",
                    loginPageUrl);

            HtmlPage loginPage = webClient.getPage(loginPageUrl);
            HtmlAnchor link = loginPage.getHtmlElementById("login");
            assertEquals("/tenant-restore/protected?x=1&q_login=true", link.getHrefAttribute());

            HtmlPage keycloakPage = link.click();
            String keycloakUrl = keycloakPage.getUrl().toString();
            assertTrue(keycloakUrl.contains("redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Ftenant-restore%2Fcallback&"),
                    keycloakUrl);
            assertTrue(keycloakUrl.contains("code_challenge="), keycloakUrl);

            Page page = login(keycloakPage);
            assertEquals("http://localhost:8081/tenant-restore/protected?x=1", page.getUrl().toString());
            assertEquals("alice:/tenant-restore/protected?x=1", page.getWebResponse().getContentAsString());
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testLoginPageIsPublic() throws Exception {
        try (WebClient webClient = createWebClient()) {
            HtmlPage loginPage = webClient.getPage("http://localhost:8081/login");
            assertEquals(200, loginPage.getWebResponse().getStatusCode());
            assertEquals("/protected", loginPage.<HtmlAnchor> getHtmlElementById("login").getHrefAttribute());

            WebResponse response = get(webClient, "http://localhost:8081/login?redirect=https://attacker.example.com");
            assertEquals(400, response.getStatusCode());
        }
    }

    @Test
    public void testProtectedLoginPageIsRejected() throws Exception {
        try (WebClient webClient = createWebClient()) {
            WebResponse challenge = get(webClient, "http://localhost:8081/tenant-protected-login/protected");
            assertEquals(302, challenge.getStatusCode());
            String loginPageUrl = challenge.getResponseHeaderValue("location");
            assertEquals("http://localhost:8081/tenant-protected-login/login"
                    + "?redirect=%2Ftenant-protected-login%2Fprotected%3Fq_login%3Dtrue&login-page-filter=applied",
                    loginPageUrl);

            WebResponse loginPage = get(webClient, loginPageUrl);
            assertEquals(401, loginPage.getStatusCode());
            assertEquals("OIDC", loginPage.getResponseHeaderValue("WWW-Authenticate"));
            assertNull(loginPage.getResponseHeaderValue("location"));
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testJavaScriptRequestIsNotRedirected() throws Exception {
        try (WebClient webClient = createWebClient()) {
            webClient.addRequestHeader("X-Requested-With", "JavaScript");
            WebResponse response = get(webClient, "http://localhost:8081/tenant-strict/protected");
            assertEquals(499, response.getStatusCode());
            assertEquals("OIDC", response.getResponseHeaderValue("WWW-Authenticate"));
            assertNull(response.getResponseHeaderValue("location"));
            webClient.getCookieManager().clearCookies();
        }
    }

    static WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    static WebResponse get(WebClient webClient, String url) throws Exception {
        webClient.getOptions().setRedirectEnabled(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        try {
            return webClient.loadWebResponse(new WebRequest(URI.create(url).toURL()));
        } finally {
            webClient.getOptions().setRedirectEnabled(true);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);
        }
    }

    static Page login(HtmlPage keycloakPage) throws Exception {
        HtmlForm form = keycloakPage.getForms().get(0);
        form.getInputByName("username").setValueAttribute("alice");
        form.getInputByName("password").setValueAttribute("alice");
        return form.getButtonByName("login").click();
    }

    static void addCookie(WebClient webClient, String name, String value) {
        webClient.getCookieManager().addCookie(new Cookie("localhost", name, value));
    }

    static boolean hasCookie(WebClient webClient, String namePrefix) {
        return webClient.getCookieManager().getCookies().stream().anyMatch(c -> c.getName().startsWith(namePrefix));
    }
}
