package io.quarkus.it.csrf;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@QuarkusTest
public class CsrfReactiveTest {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    @TestHTTPResource
    URL url;

    @Test
    public void testCsrfTokenInForm() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            webClient.addRequestHeader("Authorization", basicAuth("alice", "alice"));
            HtmlPage htmlPage = webClient.getPage("http://localhost:8081/service/csrfTokenForm");
            htmlPage = webClient.getPage("http://localhost:8081/service/csrfTokenForm");
            assertNotNull(htmlPage.getWebResponse().getResponseHeaderValue("Set-Cookie"));

            assertEquals("CSRF Token Form Test", htmlPage.getTitleText());

            HtmlForm loginForm = htmlPage.getForms().get(0);

            loginForm.getInputByName("name").setValueAttribute("alice");

            assertNotNull(webClient.getCookieManager().getCookie("csrftoken"));

            TextPage textPage = loginForm.getInputByName("submit").click();
            assertNotNull(htmlPage.getWebResponse().getResponseHeaderValue("Set-Cookie"));
            assertEquals("alice:true:tokenHeaderIsSet=false", textPage.getContent());

            Cookie cookie1 = webClient.getCookieManager().getCookie("csrftoken");

            // This request which returns String is not CSRF protected
            textPage = webClient.getPage("http://localhost:8081/service/hello");
            assertEquals("hello", textPage.getContent());
            // therefore no Set-Cookie header is expected
            assertNull(textPage.getWebResponse().getResponseHeaderValue("Set-Cookie"));

            // Repeat a form submission
            textPage = loginForm.getInputByName("submit").click();
            assertNotNull(htmlPage.getWebResponse().getResponseHeaderValue("Set-Cookie"));
            assertEquals("alice:true:tokenHeaderIsSet=false", textPage.getContent());

            Cookie cookie2 = webClient.getCookieManager().getCookie("csrftoken");

            assertEquals(cookie1.getValue(), cookie2.getValue());
            assertTrue(cookie1.getExpires().before(cookie2.getExpires()));

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testCsrfTokenTwoForms() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            webClient.addRequestHeader("Authorization", basicAuth("alice", "alice"));
            HtmlPage htmlPage = webClient.getPage("http://localhost:8081/service/csrfTokenFirstForm");

            assertEquals("CSRF Token First Form Test", htmlPage.getTitleText());

            HtmlForm loginForm = htmlPage.getForms().get(0);

            loginForm.getInputByName("name").setValueAttribute("alice");

            assertNotNull(webClient.getCookieManager().getCookie("csrftoken"));

            htmlPage = loginForm.getInputByName("submit").click();

            assertEquals("CSRF Token Second Form Test", htmlPage.getTitleText());

            loginForm = htmlPage.getForms().get(0);

            loginForm.getInputByName("name").setValueAttribute("alice");

            TextPage textPage = loginForm.getInputByName("submit").click();
            assertNotNull(webClient.getCookieManager().getCookie("csrftoken"));
            assertEquals("alice:true:tokenHeaderIsSet=false", textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testCsrfTokenWithFormRead() throws Exception {
        try (final WebClient webClient = createWebClient()) {

            HtmlPage htmlPage = webClient.getPage("http://localhost:8081/service/csrfTokenWithFormRead");

            assertEquals("CSRF Token With Form Read Test", htmlPage.getTitleText());

            HtmlForm loginForm = htmlPage.getForms().get(0);

            loginForm.getInputByName("name").setValueAttribute("alice");

            assertNotNull(webClient.getCookieManager().getCookie("csrftoken"));

            TextPage textPage = loginForm.getInputByName("submit").click();

            assertEquals("verified:true:tokenHeaderIsSet=false", textPage.getContent());

            textPage = webClient.getPage("http://localhost:8081/service/hello");
            assertEquals("hello", textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testCsrfTokenInFormButNoCookie() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            webClient.addRequestHeader("Authorization", basicAuth("alice", "alice"));
            HtmlPage htmlPage = webClient.getPage("http://localhost:8081/service/csrfTokenForm");

            assertEquals("CSRF Token Form Test", htmlPage.getTitleText());

            HtmlForm loginForm = htmlPage.getForms().get(0);

            loginForm.getInputByName("name").setValueAttribute("alice");
            assertNotNull(webClient.getCookieManager().getCookie("csrftoken"));

            webClient.getCookieManager().clearCookies();

            assertNull(webClient.getCookieManager().getCookie("csrftoken"));
            try {
                loginForm.getInputByName("submit").click();
                fail("400 status error is expected");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(400, ex.getStatusCode());
            }
            webClient.getCookieManager().clearCookies();

        }
    }

    public void testCsrfFailedAuthentication() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            webClient.addRequestHeader("Authorization", basicAuth("alice", "password"));
            try {
                webClient.getPage("http://localhost:8081/service/csrfTokenForm");
                fail("401 status error is expected");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(401, ex.getStatusCode());
                assertEquals("true", ex.getResponse().getResponseHeaderValue("test-mapper"));
                assertNull(webClient.getCookieManager().getCookie("csrftoken"));
            }
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testCsrfTokenInMultipart() throws Exception {
        try (final WebClient webClient = createWebClient()) {

            HtmlPage htmlPage = webClient.getPage("http://localhost:8081/service/csrfTokenMultipart");

            assertEquals("CSRF Token Multipart Test", htmlPage.getTitleText());

            HtmlForm loginForm = htmlPage.getForms().get(0);

            loginForm.getInputByName("name").setValueAttribute("alice");
            loginForm.getInputByName("file").setValueAttribute("file.txt");

            assertNotNull(webClient.getCookieManager().getCookie("csrftoken"));

            TextPage textPage = loginForm.getInputByName("submit").click();

            assertEquals("alice:true:true:true:tokenHeaderIsSet=false", textPage.getContent());

            textPage = webClient.getPage("http://localhost:8081/service/hello");
            assertEquals("hello", textPage.getContent());

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testWrongCsrfTokenCookieValue() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            webClient.addRequestHeader("Authorization", basicAuth("alice", "alice"));
            HtmlPage htmlPage = webClient.getPage("http://localhost:8081/service/csrfTokenForm");

            assertEquals("CSRF Token Form Test", htmlPage.getTitleText());

            HtmlForm loginForm = htmlPage.getForms().get(0);

            loginForm.getInputByName("name").setValueAttribute("alice");
            assertNotNull(webClient.getCookieManager().getCookie("csrftoken"));

            webClient.getCookieManager().clearCookies();

            assertNull(webClient.getCookieManager().getCookie("csrftoken"));

            webClient.getCookieManager().addCookie(new Cookie("localhost", "csrftoken", "wrongvalue"));

            assertNotNull(webClient.getCookieManager().getCookie("csrftoken"));
            try {
                loginForm.getInputByName("submit").click();
                fail("400 status error is expected");
            } catch (FailingHttpStatusCodeException ex) {
                assertEquals(400, ex.getStatusCode());
            }
            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testWrongCsrfTokenFormValue() throws Exception {
        try (final WebClient webClient = createWebClient()) {
            webClient.addRequestHeader("Authorization", basicAuth("alice", "alice"));
            HtmlPage htmlPage = webClient.getPage("http://localhost:8081/service/csrfTokenForm");

            assertEquals("CSRF Token Form Test", htmlPage.getTitleText());

            assertNotNull(webClient.getCookieManager().getCookie("csrftoken"));

            RestAssured.given().urlEncodingEnabled(true)
                    .param("csrf-token", "wrong-value")
                    .post("/service/csrfTokenForm")
                    .then().statusCode(400);

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testCsrfTokenHeaderValue() throws Exception {
        Vertx vertx = Vertx.vertx();
        io.vertx.ext.web.client.WebClient vertxWebClient = io.vertx.ext.web.client.WebClient.create(vertx);
        try {
            try (final WebClient webClient = createWebClient()) {

                HtmlPage htmlPage = webClient.getPage("http://localhost:8081/service/csrfTokenWithHeader");
                assertEquals("CSRF Token Header Test", htmlPage.getTitleText());
                List<DomElement> inputs = htmlPage.getElementsByIdAndOrName("X-CSRF-TOKEN");
                String csrfToken = inputs.get(0).asNormalizedText();

                Cookie csrfCookie = webClient.getCookieManager().getCookie("csrftoken");
                assertNotNull(csrfCookie);

                assurePostFormPath(vertxWebClient, "/service/csrfTokenWithHeader", 200, csrfCookie,
                        csrfToken, "verified:true:tokenHeaderIsSet=true");
                assurePostFormPath(vertxWebClient, "//service/csrfTokenWithHeader", 200, csrfCookie,
                        csrfToken, "verified:true:tokenHeaderIsSet=true");

                webClient.getCookieManager().clearCookies();
            }
        } finally {
            closeVertxWebClient(vertxWebClient, vertx);
        }
    }

    @Test
    public void testCsrfTokenHeaderValueJson() throws Exception {
        Vertx vertx = Vertx.vertx();
        io.vertx.ext.web.client.WebClient vertxWebClient = io.vertx.ext.web.client.WebClient.create(vertx);
        try {
            try (final WebClient webClient = createWebClient()) {

                HtmlPage htmlPage = webClient.getPage("http://localhost:8081/service/csrfTokenWithHeader");
                assertEquals("CSRF Token Header Test", htmlPage.getTitleText());
                List<DomElement> inputs = htmlPage.getElementsByIdAndOrName("X-CSRF-TOKEN");
                String csrfToken = inputs.get(0).asNormalizedText();

                Cookie csrfCookie = webClient.getCookieManager().getCookie("csrftoken");
                assertNotNull(csrfCookie);

                assurePostJsonPath(vertxWebClient, "/service/csrfTokenWithHeader", 200, csrfCookie,
                        csrfToken, "verified:true:tokenHeaderIsSet=true");
                assurePostJsonPath(vertxWebClient, "//service/csrfTokenWithHeader", 200, csrfCookie,
                        csrfToken, "verified:true:tokenHeaderIsSet=true");

                webClient.getCookieManager().clearCookies();
            }
        } finally {
            closeVertxWebClient(vertxWebClient, vertx);
        }
    }

    @Test
    public void testWrongCsrfTokenHeaderValue() throws Exception {
        Vertx vertx = Vertx.vertx();
        io.vertx.ext.web.client.WebClient vertxWebClient = io.vertx.ext.web.client.WebClient.create(vertx);
        try {
            try (final WebClient webClient = createWebClient()) {

                HtmlPage htmlPage = webClient.getPage("http://localhost:8081/service/csrfTokenWithHeader");
                assertEquals("CSRF Token Header Test", htmlPage.getTitleText());

                Cookie csrfCookie = webClient.getCookieManager().getCookie("csrftoken");
                assertNotNull(csrfCookie);

                // CSRF cookie is signed, so passing it as a header value will fail
                assurePostFormPath(vertxWebClient, "/service/csrfTokenWithHeader", 400, csrfCookie,
                        csrfCookie.getValue(), null);
                assurePostFormPath(vertxWebClient, "//service/csrfTokenWithHeader", 400, csrfCookie,
                        csrfCookie.getValue(), null);
                webClient.getCookieManager().clearCookies();
            }
        } finally {
            closeVertxWebClient(vertxWebClient, vertx);
        }
    }

    @Test
    public void testWrongCsrfTokenWithFormRead() throws Exception {
        try (final WebClient webClient = createWebClient()) {

            HtmlPage htmlPage = webClient.getPage("http://localhost:8081/service/csrfTokenWithFormRead");

            assertEquals("CSRF Token With Form Read Test", htmlPage.getTitleText());

            assertNotNull(webClient.getCookieManager().getCookie("csrftoken"));

            RestAssured.given().urlEncodingEnabled(true)
                    .param("csrf-token", "wrong-value")
                    .post("/service/csrfTokenWithFormRead")
                    .then().statusCode(400);

            webClient.getCookieManager().clearCookies();
        }
    }

    @Test
    public void testGetWithCsrfToken() throws Exception {
        try (final WebClient webClient = createWebClient()) {

            assertNull(webClient.getCookieManager().getCookie("csrftoken"));

            TextPage htmlPage = webClient.getPage("http://localhost:8081/service/token");

            assertNotNull(webClient.getCookieManager().getCookie("csrftoken"));

            // Can't check that it matches the cookie because it's signed
            assertNotNull(htmlPage.getContent());

            // get it again
            htmlPage = webClient.getPage("http://localhost:8081/service/token");

            assertNotNull(webClient.getCookieManager().getCookie("csrftoken"));

            // Can't check that it matches the cookie because it's signed
            assertNotNull(htmlPage.getContent());

            webClient.getCookieManager().clearCookies();
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private String basicAuth(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
    }

    private void assurePostFormPath(io.vertx.ext.web.client.WebClient vertxWebClient, String path,
            int expectedStatus, Cookie csrfCookie, String csrfToken, String responseBody) {
        var req = vertxWebClient.post(url.getPort(), url.getHost(), path);
        req.basicAuthentication("alice", "alice");
        req.putHeader("X-CSRF-TOKEN", csrfToken);
        req.putHeader("Cookie", csrfCookie.getName() + "=" + csrfCookie.getValue());

        var result = req.sendForm(io.vertx.core.MultiMap.caseInsensitiveMultiMap()
                .add("csrf-header", "X-CSRF-TOKEN"));

        await().atMost(REQUEST_TIMEOUT).until(result::isComplete);
        assertEquals(expectedStatus, result.result().statusCode(), path);
        if (responseBody != null) {
            assertEquals(responseBody, result.result().bodyAsString(), path);
        }
        if (expectedStatus != 400) {
            String[] nextCookie = result.result().cookies().get(0).split(";");
            String[] cookieNameValue = nextCookie[0].trim().split("=");
            assertEquals(csrfCookie.getName(), cookieNameValue[0]);
            assertEquals(csrfCookie.getValue(), cookieNameValue[1]);
        }
    }

    private void assurePostJsonPath(io.vertx.ext.web.client.WebClient vertxWebClient, String path,
            int expectedStatus, Cookie csrfCookie, String csrfToken, String responseBody) {
        var req = vertxWebClient.post(url.getPort(), url.getHost(), path);
        req.basicAuthentication("alice", "alice");
        req.putHeader("X-CSRF-TOKEN", csrfToken);
        req.putHeader("Cookie", csrfCookie.getName() + "=" + csrfCookie.getValue());

        var result = req.sendJson(new JsonObject("{}"));

        await().atMost(REQUEST_TIMEOUT).until(result::isComplete);
        assertEquals(expectedStatus, result.result().statusCode(), path);
        if (responseBody != null) {
            assertEquals(responseBody, result.result().bodyAsString(), path);
        }
    }

    private static void closeVertxWebClient(io.vertx.ext.web.client.WebClient vertxWebClient, Vertx vertx) {
        if (vertxWebClient != null) {
            vertxWebClient.close();
        }
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
    }
}
