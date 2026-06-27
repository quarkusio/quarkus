package io.quarkus.email.authentication.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.response.ValidatableResponse;

final class TestEmailAuthenticationHelper {

    static final String FROM = "security@quarkus.io";

    private final String codeGenerationPath;
    private final String postLocation;
    private final String emailSubject;
    private final String emailTextStart;
    private CookieFilter cookieFilter;
    private final String emailParameter;
    private final String codeParameter;

    TestEmailAuthenticationHelper(String codeGenerationPath, String postLocation, String emailSubject, String emailTextStart,
            String emailParameter, String codeParameter) {
        this.codeGenerationPath = codeGenerationPath;
        this.postLocation = postLocation;
        this.emailSubject = emailSubject;
        this.emailTextStart = emailTextStart;
        this.emailParameter = emailParameter;
        this.codeParameter = codeParameter;
        this.cookieFilter = new CookieFilter();
    }

    ValidatableResponse requestCodeFor(String username) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        return RestAssured.given()
                .filter(cookieFilter)
                .formParam(emailParameter, username)
                .post(codeGenerationPath)
                .then();
    }

    void clear() {
        cookieFilter = new CookieFilter();
        TestEmailIdentityAugmentor.reset();
    }

    String assertEmailAndGetCode(MockMailbox mailbox, String emailAddress) {
        var emails = awaitEmailWithCode(mailbox, emailAddress);
        var firstMailAsserter = assertThat(emails).hasSize(1).first();
        firstMailAsserter.extracting(Mail::getSubject).isEqualTo(emailSubject);
        assertThat(firstMailAsserter.extracting(Mail::getText).actual()).startsWith(emailTextStart);
        String emailAuthCode = extractCode(emails.get(0).getText());
        assertThat(emailAuthCode).isNotEmpty();
        return emailAuthCode;
    }

    static String extractCode(String emailText) {
        return emailText
                .trim()
                .transform(t -> {
                    var arr = t.split(" ");
                    return arr[arr.length - 1];
                });
    }

    Consumer<JavaArchive> getNamedMailAppConfig(String mailerName, Class<?>... classes) {
        return (jar) -> jar
                .addAsResource(new StringAsset("""
                        quarkus.mailer%s.from=%s
                        quarkus.http.auth.permission.secured.paths=/secured*
                        quarkus.http.auth.permission.secured.policy=authenticated
                        quarkus.http.auth.permission.admin.paths=/secured/admin*
                        quarkus.http.auth.permission.admin.policy=admin
                        quarkus.http.auth.policy.admin.roles-allowed=admin
                        """.formatted(mailerName.isEmpty() ? "" : "." + mailerName, FROM)), "application.properties")
                .addClasses(classes)
                .addClasses(TestEmailIdentityAugmentor.class, TestEmailAuthenticationHelper.class, TestPathHandler.class);
    }

    Consumer<JavaArchive> getAppConfig(Class<?>... classes) {
        return getNamedMailAppConfig("", classes);
    }

    ValidatableResponse goTo(String targetPath) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        return RestAssured
                .given()
                .redirects().follow(false)
                .filter(cookieFilter)
                .get(targetPath)
                .then();
    }

    Cookie assertCookiePresent(String cookieName) {
        return assertCookie(cookieName, null);
    }

    Cookie assertCookie(String cookieName, String value) {
        var cookieAsserter = assertThat(cookieFilter.getCookieStore().getCookies());
        cookieAsserter.isNotEmpty();
        cookieAsserter.extracting(Cookie::getName).anyMatch(cookieName::equalsIgnoreCase);
        var cookie = cookieAsserter.actual().stream().filter(c -> cookieName.equalsIgnoreCase(c.getName()))
                .findFirst().get();
        var cookieValueAsserter = assertThat(cookie).extracting(Cookie::getValue).asString();
        if (value == null) {
            cookieValueAsserter.isNotEmpty();
        } else {
            cookieValueAsserter.contains(value);
        }
        return cookie;
    }

    ValidatableResponse submitCode(String code) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        return RestAssured.given()
                .filter(cookieFilter)
                .redirects().follow(false)
                .formParam(codeParameter, code)
                .post(postLocation)
                .then();
    }

    void assertCookieMissing(String cookieName) {
        assertThat(cookieFilter.getCookieStore().getCookies()).extracting(Cookie::getName)
                .noneMatch(cookieName::equalsIgnoreCase);
    }

    void addCookie(String cookieName, String cookieValue) {
        var cookie = new BasicClientCookie(cookieName, cookieValue);
        cookieFilter.getCookieStore().addCookie(cookie);
    }

    private static List<Mail> awaitEmailWithCode(MockMailbox mailbox, String address) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> mailbox.getMailsSentTo(address), Matchers.not(Matchers.emptyIterable()));
    }

}
