package io.quarkus.it.csrf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class CsrfReactiveTest {

    @Test
    public void testCsrfTokenInForm() throws Exception {
        try (final WebClient webClient = createWebClient()) {

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
    public void testCsrfTokenInFormButNoCookie() throws Exception {
        try (final WebClient webClient = createWebClient()) {

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

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }
}
