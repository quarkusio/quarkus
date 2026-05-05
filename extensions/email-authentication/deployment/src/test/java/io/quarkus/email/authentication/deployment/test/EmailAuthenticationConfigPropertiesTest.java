package io.quarkus.email.authentication.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.time.temporal.ChronoUnit;
import java.util.Date;

import jakarta.inject.Inject;

import org.apache.http.cookie.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.QuarkusUnitTest;

class EmailAuthenticationConfigPropertiesTest {

    private static final TestEmailAuthenticationHelper HELPER = new TestEmailAuthenticationHelper(
            "/secured/gen-code", "/secured/post-location", "code is here!", "code is ",
            "mail-param", "code-param");

    @RegisterExtension
    static final QuarkusUnitTest APP = new QuarkusUnitTest()
            .withApplicationRoot(HELPER.getNamedMailAppConfig("named-1", TestSecurityEventObserver.class))
            .overrideConfigKey("quarkus.email-authentication.mailer-name", "named-1")
            .withRuntimeConfiguration("""
                    quarkus.email-authentication.email-text=code is %s
                    quarkus.email-authentication.email-subject=code is here!
                    quarkus.email-authentication.code-cookie=code-req
                    quarkus.email-authentication.code-expires-in=3s
                    quarkus.email-authentication.code-generation-location=secured/gen-code
                    quarkus.email-authentication.code-length=7
                    quarkus.email-authentication.code-parameter=code-param
                    quarkus.email-authentication.code-page=secured/code-form.html
                    quarkus.email-authentication.priority=2001
                    quarkus.email-authentication.post-location=secured/post-location
                    quarkus.email-authentication.cookie-same-site=lax
                    quarkus.email-authentication.http-only-cookie=false
                    quarkus.email-authentication.cookie-domain=localhost
                    quarkus.email-authentication.cookie-path=/secured
                    quarkus.email-authentication.session-cookie=quarkus-session-cookie
                    quarkus.email-authentication.location-cookie=quarkus-redir-link
                    quarkus.email-authentication.landing-page=secured
                    quarkus.email-authentication.error-page=err.html
                    quarkus.email-authentication.email-parameter=mail-param
                    quarkus.email-authentication.login-page=log-me-in.html
                    """);

    @Inject
    MockMailbox mailbox;

    @Inject
    TestSecurityEventObserver eventObserver;

    @BeforeEach
    void reset() {
        HELPER.clear();
        eventObserver.clear();
        mailbox.clear();
    }

    @Test
    void testCompleteFlowSuccess() {
        String emailAddress = "test-vaclav@quarkus.io";
        TestEmailIdentityAugmentor.addUser(emailAddress, "admin");

        // go to secured page and expect redirection to login page where we can submit username
        String targetPath = "/secured/admin";
        HELPER.goTo(targetPath).statusCode(302).header("location", containsString("/log-me-in.html"));
        var cookie = HELPER.assertCookie("quarkus-redir-link", targetPath);
        assertThat(cookie).extracting(Cookie::getDomain).asString().isEqualTo("localhost");
        assertThat(cookie).extracting(Cookie::getPath).asString().isEqualTo("/secured");

        // request code and expect redirection to a form where we can submit code
        var expectedCodeCookieExpiration = new Date().toInstant().plus(3, ChronoUnit.SECONDS);
        HELPER.requestCodeFor(emailAddress).statusCode(302).header("location", containsString("/secured/code-form.html"));
        cookie = HELPER.assertCookiePresent("code-req");
        assertThat(cookie.getExpiryDate()).isAfterOrEqualTo(expectedCodeCookieExpiration);
        assertThat(cookie).extracting(Cookie::getDomain).asString().isEqualTo("localhost");
        assertThat(cookie).extracting(Cookie::getPath).asString().isEqualTo("/secured");
        String code = HELPER.assertEmailAndGetCode(mailbox, emailAddress);
        assertThat(code).hasSize(7);
        for (char c : code.toCharArray()) { // we only allow certain characters to be in the generated code
            assertThat(Character.toString(c)).isSubstringOf("23456789BCDFGHJKMNPQRSTVWXYZ");
        }

        // login with code and then get session cookie
        HELPER.submitCode(code).statusCode(302).header("location", containsString(targetPath));
        HELPER.assertCookieMissing("code-req");
        HELPER.assertCookieMissing("quarkus-redir-link");
        HELPER.assertCookiePresent("quarkus-session-cookie");

        // use the session cookie to access a path that requires 'admin' role
        HELPER.goTo(targetPath).statusCode(200).body(is(emailAddress + ":" + targetPath));
    }

    @Test
    void testValidCodeWithoutRequestCookie() {
        String email = "albus@quarkus.io";

        HELPER.requestCodeFor(email).statusCode(302).header("location", containsString("/secured/code-form.html"));
        HELPER.assertCookiePresent("code-req");
        String code = HELPER.assertEmailAndGetCode(mailbox, email);
        assertThat(code).hasSize(7);

        HELPER.clear();
        HELPER.assertCookieMissing("code-req");

        HELPER.submitCode(code).statusCode(302).header("location", containsString("/err.html"));
    }

    @Test
    void testCodeExpirationTime() throws InterruptedException {
        String email = "albus@quarkus.io";

        HELPER.requestCodeFor(email).statusCode(302).header("location", containsString("/secured/code-form.html"));
        HELPER.assertCookiePresent("code-req");
        String code = HELPER.assertEmailAndGetCode(mailbox, email);
        assertThat(code).hasSize(7);

        // code should expire in 3 seconds
        Thread.sleep(3300);

        HELPER.submitCode(code).statusCode(302).header("location", containsString("/err.html"));
    }
}
