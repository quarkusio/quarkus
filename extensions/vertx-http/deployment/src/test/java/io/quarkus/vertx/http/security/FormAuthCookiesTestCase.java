package io.quarkus.vertx.http.security;

import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;

public class FormAuthCookiesTestCase {

    private static final String APP_PROPS = "" +
            "quarkus.http.auth.form.enabled=true\n" +
            "quarkus.http.auth.form.login-page=login\n" +
            "quarkus.http.auth.form.error-page=error\n" +
            "quarkus.http.auth.form.landing-page=landing\n" +
            "quarkus.http.auth.policy.r1.roles-allowed=admin\n" +
            "quarkus.http.auth.permission.roles1.paths=/admin%E2%9D%A4\n" +
            "quarkus.http.auth.permission.roles1.policy=r1\n" +
            "quarkus.http.auth.form.timeout=PT2S\n" +
            "quarkus.http.auth.form.new-cookie-interval=PT1S\n" +
            "quarkus.http.auth.form.cookie-name=laitnederc-sukrauq\n" +
            "quarkus.http.auth.form.cookie-same-site=lax\n" +
            "quarkus.http.auth.form.http-only-cookie=true\n" +
            "quarkus.http.auth.form.cookie-max-age=PT2M\n" +
            "quarkus.http.auth.session.encryption-key=CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class, TestTrustedIdentityProvider.class,
                            PathHandler.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
    }

    @Test
    public void testFormBasedAuthSuccess() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/admin❤")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/login"))
                .cookie("quarkus-redirect-location", detailedCookie().value(containsString("/admin%E2%9D%A4")).sameSite("Lax"));

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "admin")
                .formParam("j_password", "admin")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/admin%E2%9D%A4"))
                .cookie("laitnederc-sukrauq", detailedCookie().value(notNullValue())
                        .httpOnly(true).sameSite("Lax").maxAge(120));

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/admin❤")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("admin:/admin%E2%9D%A4"));

    }

    private String getCredentialCookie(CookieStore cookieStore) {
        for (Cookie cookie : cookieStore.getCookies()) {
            if ("laitnederc-sukrauq".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void doRegularGet(CloseableHttpClient httpClient, CookieStore cookieStore, String credentialCookieValue)
            throws IOException {
        HttpGet httpGet = new HttpGet(url.toString() + "/admin%E2%9D%A4");
        try (CloseableHttpResponse adminResponse = httpClient.execute(httpGet)) {
            String credentialInCookieStore = getCredentialCookie(cookieStore);
            assertEquals(credentialCookieValue, credentialInCookieStore,
                    "Session cookie WAS NOT eligible for renewal and should have remained the same.");
            assertEquals(200, adminResponse.getStatusLine().getStatusCode(), "HTTP 200 expected.");
            assertEquals("admin:/admin%E2%9D%A4", EntityUtils.toString(adminResponse.getEntity(), "UTF-8"),
                    "Unexpected web page content.");
        }
    }

    private void waitForPointInTime(long pointInFuture) throws InterruptedException {
        long wait = pointInFuture - System.currentTimeMillis();
        assertTrue(wait > 0, "Having to wait for " + wait
                + " ms for another request is unexpected. The previous one took too long.");
        Thread.sleep(wait);
    }

    @TestHTTPResource
    URL url;

    @Test
    @Disabled("The logic in this test case relies too heavily on the current system time and can result in spurious failures on slow systems. See https://github.com/quarkusio/quarkus/issues/10106")
    public void testCredentialCookieRotation() throws IOException, InterruptedException {

        final CookieStore cookieStore = new BasicCookieStore();

        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultCookieStore(cookieStore)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build()) {

            final List<NameValuePair> authForm = new ArrayList<>();
            authForm.add(new BasicNameValuePair("j_username", "admin"));
            authForm.add(new BasicNameValuePair("j_password", "admin"));
            final UrlEncodedFormEntity entity = new UrlEncodedFormEntity(authForm, Consts.UTF_8);

            // Login
            HttpPost httpPost = new HttpPost(url.toString() + "/j_security_check");
            httpPost.setEntity(entity);
            String credentialCookieValue = null;
            try (CloseableHttpResponse loginResponse = httpClient.execute(httpPost)) {
                assertEquals(302, loginResponse.getStatusLine().getStatusCode(),
                        "Login should have been successful and return HTTP 302 redirect.");
                credentialCookieValue = getCredentialCookie(cookieStore);
                assertTrue(StringUtils.isNotBlank(credentialCookieValue), "Credential cookie value must not be blank.");
            }

            long t0 = System.currentTimeMillis();

            waitForPointInTime(t0 + 400);

            doRegularGet(httpClient, cookieStore, credentialCookieValue);

            waitForPointInTime(t0 + 700);

            doRegularGet(httpClient, cookieStore, credentialCookieValue);

            waitForPointInTime(t0 + 1300);

            HttpGet httpGet = new HttpGet(url.toString() + "/admin%E2%9D%A4");
            try (CloseableHttpResponse adminResponse = httpClient.execute(httpGet)) {
                String credentialInCookieStore = getCredentialCookie(cookieStore);
                assertNotEquals(credentialCookieValue, credentialInCookieStore,
                        "Session cookie WAS eligible for renewal and should have been updated.");
                assertEquals(200, adminResponse.getStatusLine().getStatusCode(), "HTTP 200 expected.");
                assertEquals("admin:/admin%E2%9D%A4", EntityUtils.toString(adminResponse.getEntity(), "UTF-8"),
                        "Unexpected web page content.");

                credentialCookieValue = credentialInCookieStore;

            }

            t0 = System.currentTimeMillis();

            waitForPointInTime(t0 + 400);

            doRegularGet(httpClient, cookieStore, credentialCookieValue);

            waitForPointInTime(t0 + 700);

            doRegularGet(httpClient, cookieStore, credentialCookieValue);

            waitForPointInTime(t0 + 3600);

            httpGet = new HttpGet(url.toString() + "/admin%E2%9D%A4");
            try (CloseableHttpResponse adminResponse = httpClient.execute(httpGet)) {
                assertEquals(200, adminResponse.getStatusLine().getStatusCode(), "HTTP 200 from login page expected.");
                assertEquals(":/login", EntityUtils.toString(adminResponse.getEntity(), "UTF-8"),
                        "Login web page was expected. Quarkus should have enforced a new login.");
                String redirectLocation = null;
                for (Cookie cookie : cookieStore.getCookies()) {
                    if ("quarkus-redirect-location".equals(cookie.getName())) {
                        redirectLocation = cookie.getValue();
                        break;
                    }
                }
                assertTrue(StringUtils.isNotBlank(redirectLocation) && redirectLocation.contains("admin%E2%9D%A4"),
                        "quarkus-redirect-location should have been set.");
            }

            httpPost = new HttpPost(url.toString() + "/j_security_check");
            httpPost.setEntity(entity);
            try (CloseableHttpResponse loginResponse = httpClient.execute(httpPost)) {
                assertEquals(302, loginResponse.getStatusLine().getStatusCode(),
                        "Login should have been successful and return HTTP 302 redirect.");
                String newCredentialCookieValue = getCredentialCookie(cookieStore);
                assertTrue(StringUtils.isNotBlank(newCredentialCookieValue), "Credential cookie value must not be blank.");
                assertNotEquals(newCredentialCookieValue, credentialCookieValue,
                        "New credential cookie must not be the same as the previous one.");
            }
        }
    }
}
