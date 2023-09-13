package io.quarkus.it.csrf;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
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

            assertEquals("CSRF Token Form Test", htmlPage.getTitleText());

            HtmlForm loginForm = htmlPage.getForms().get(0);

            loginForm.getInputByName("name").setValueAttribute("alice");

            assertNotNull(webClient.getCookieManager().getCookie("csrftoken"));

            TextPage textPage = loginForm.getInputByName("submit").click();

            assertEquals("alice:true", textPage.getContent());

            textPage = webClient.getPage("http://localhost:8081/service/hello");
            assertEquals("hello", textPage.getContent());

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

            assertEquals("verified:true", textPage.getContent());

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

            assertEquals("alice:true:true:true", textPage.getContent());

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
