package io.quarkus.email.authentication.deployment.test;

import static io.quarkus.email.authentication.EmailAuthenticationEvent.AUTHENTICATION_CODE_KEY;
import static io.quarkus.email.authentication.EmailAuthenticationEvent.FAILURE_KEY;
import static io.quarkus.email.authentication.EmailAuthenticationEvent.EmailAuthenticationEventType.AUTHENTICATION_CODE;
import static io.quarkus.email.authentication.EmailAuthenticationEvent.EmailAuthenticationEventType.EMAIL_LOGIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.time.temporal.ChronoUnit;
import java.util.Date;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.email.authentication.EmailAuthenticationEvent;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.spi.runtime.AbstractSecurityEvent;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
import io.quarkus.test.QuarkusUnitTest;

class EmailAuthenticationDefaultsTest {

    private static final TestEmailAuthenticationHelper HELPER = new TestEmailAuthenticationHelper(
            "/generate-email-authentication-code", "/j_security_check", "Your verification code", "Your verification code is",
            "email", "code");

    @RegisterExtension
    static final QuarkusUnitTest APP = new QuarkusUnitTest()
            .withApplicationRoot(HELPER.getAppConfig(TestSecurityEventObserver.class));

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
        HELPER.goTo(targetPath).statusCode(302).header("location", containsString("/login.html"));
        HELPER.assertCookie("quarkus-redirect-location", targetPath);

        // request code and expect redirection to a form where we can submit code
        var expectedCodeCookieExpiration = new Date().toInstant().plus(5, ChronoUnit.MINUTES);
        HELPER.requestCodeFor(emailAddress).statusCode(302).header("location", containsString("/code.html"));
        var cookie = HELPER.assertCookiePresent("quarkus-credential-request");
        assertThat(cookie.getExpiryDate()).isAfterOrEqualTo(expectedCodeCookieExpiration);
        String code = HELPER.assertEmailAndGetCode(mailbox, emailAddress);
        assertThat(code).hasSize(15);
        for (char c : code.toCharArray()) { // we only allow certain characters to be in the generated code
            assertThat(Character.toString(c)).isSubstringOf("23456789BCDFGHJKMNPQRSTVWXYZ");
        }

        // login with code and then get session cookie
        HELPER.submitCode(code).statusCode(302).header("location", containsString(targetPath));
        HELPER.assertCookieMissing("quarkus-credential-request");
        HELPER.assertCookieMissing("quarkus-redirect-location");
        HELPER.assertCookiePresent("quarkus-credential");

        // use the session cookie to access a path that requires 'admin' role
        HELPER.goTo(targetPath).statusCode(200).body(is(emailAddress + ":" + targetPath));
    }

    @Test
    void testInvalidEmail() {
        HELPER.requestCodeFor("invalid-email").statusCode(302).header("location", containsString("/error.html"));
    }

    @Test
    void testSwitchUser() {
        String mail1 = "test-vaclav@quarkus.io";
        String mail2 = "test-martin@quarkus.io";
        TestEmailIdentityAugmentor.addUser(mail2, "admin");

        // request code and expect redirection to a form where we can submit code
        HELPER.requestCodeFor(mail1).statusCode(302).header("location", containsString("/code.html"));
        String code = HELPER.assertEmailAndGetCode(mailbox, mail1);
        assertThat(code).hasSize(15);

        // we should receive security event with the code
        Awaitility.await().untilAsserted(() -> assertThat(eventObserver.getEmailAuthenticationEvents()).isNotEmpty());
        var eventAsserter = assertThat(eventObserver.getEmailAuthenticationEvents()).hasSize(1).first();
        eventAsserter.extracting(EmailAuthenticationEvent::getEventType).isEqualTo(AUTHENTICATION_CODE);
        var eventPropertiesAsserter = assertThat(eventAsserter.actual().getEventProperties()).isNotEmpty();
        eventPropertiesAsserter.containsKey(AUTHENTICATION_CODE_KEY);
        eventPropertiesAsserter.doesNotContainKey(FAILURE_KEY);
        eventObserver.clear();

        // login with code and then get session cookie
        HELPER.submitCode(code).statusCode(302).header("location", containsString("/index.html"));
        HELPER.assertCookieMissing("quarkus-credential-request");
        HELPER.assertCookiePresent("quarkus-credential");

        // we should receive the login event
        Awaitility.await().untilAsserted(() -> assertThat(eventObserver.getEmailAuthenticationEvents()).isNotEmpty());
        eventAsserter = assertThat(eventObserver.getEmailAuthenticationEvents()).hasSize(1).first();
        eventAsserter.extracting(EmailAuthenticationEvent::getEventType).isEqualTo(EMAIL_LOGIN);
        eventPropertiesAsserter = assertThat(eventAsserter.actual().getEventProperties()).isNotEmpty();
        eventPropertiesAsserter.containsKey(AUTHENTICATION_CODE_KEY);
        eventPropertiesAsserter.doesNotContainKey(FAILURE_KEY);
        eventObserver.clear();

        // use the session cookie to access a path that requires 'admin' role
        HELPER.goTo("/secured/any").statusCode(200).body(is(mail1 + ":/secured/any"));

        // this user is missing the admin role
        HELPER.goTo("/secured/admin").statusCode(403);

        // == SWITCH USER

        // request code and expect redirection to a form where we can submit code
        HELPER.requestCodeFor(mail2).statusCode(302).header("location", containsString("/code.html"));
        String newCode = HELPER.assertEmailAndGetCode(mailbox, mail2);
        assertThat(newCode).hasSize(15).asString().isNotEqualToIgnoringCase(code);

        // login with code and then get session cookie
        HELPER.submitCode(newCode).statusCode(302).header("location", containsString("/index.html"));
        HELPER.assertCookieMissing("quarkus-credential-request");
        HELPER.assertCookiePresent("quarkus-credential");

        // use the session cookie to access a path that requires 'admin' role
        HELPER.goTo("/secured/any").statusCode(200).body(is(mail2 + ":/secured/any"));

        // this user has the admin role
        HELPER.goTo("/secured/admin").statusCode(200).body(is(mail2 + ":/secured/admin"));
    }

    @Test
    void testInvalidCodeWithoutRequestCookie() {
        testInvalidCode();
    }

    @Test
    void testInvalidCodeWithRequestCookie() {
        HELPER.requestCodeFor("albus@quarkus.io").statusCode(302).header("location", containsString("/code.html"));
        HELPER.assertCookiePresent("quarkus-credential-request");
        HELPER.assertEmailAndGetCode(mailbox, "albus@quarkus.io"); // so that we know security events arrived

        testInvalidCode();
    }

    @Test
    void testValidCodeWithoutRequestCookie() {
        String code = getCodeAndDeleteRequestCookie();

        HELPER.submitCode(code).statusCode(302).header("location", containsString("/error.html"));
    }

    @Test
    void testValidCodeWithInvalidRequestCookie() {
        String code = getCodeAndDeleteRequestCookie();

        HELPER.addCookie("quarkus-credential-request", "abcdefg");

        HELPER.submitCode(code).statusCode(302).header("location", containsString("/error.html"));
    }

    @Test
    void testValidCodeWithMismatchedValidRequestCookie() {
        String codeA = getCodeAndDeleteRequestCookie("A");

        getCode("B@quarkus.io");
        HELPER.assertCookiePresent("quarkus-credential-request");

        HELPER.submitCode(codeA).statusCode(302).header("location", containsString("/error.html"));
    }

    private String getCodeAndDeleteRequestCookie(String... postfixes) {
        String email = "albus@quarkus.io" + String.join("", postfixes);
        String code = getCode(email);

        // delete cookies
        HELPER.clear();
        HELPER.assertCookieMissing("quarkus-credential-request");

        return code;
    }

    private String getCode(String email) {
        HELPER.requestCodeFor(email).statusCode(302).header("location", containsString("/code.html"));
        HELPER.assertCookiePresent("quarkus-credential-request");
        String code = HELPER.assertEmailAndGetCode(mailbox, email);
        assertThat(code).hasSize(15);
        return code;
    }

    private void testInvalidCode() {
        HELPER.submitCode("wrong-wrong").statusCode(302).header("location", containsString("/error.html"));
        Awaitility.await().untilAsserted(() -> assertThat(eventObserver.getAuthFailedEvents()).isNotEmpty());
        var authFailedEvents = eventObserver.getAuthFailedEvents();
        var eventAsserter = assertThat(authFailedEvents).hasSize(1).first();
        eventAsserter.extracting(AbstractSecurityEvent::getSecurityIdentity).isNull();
        eventAsserter.extracting(AuthenticationFailureEvent::getAuthenticationFailure).isNotNull()
                .isInstanceOf(AuthenticationFailedException.class);
        AuthenticationFailedException exception = (AuthenticationFailedException) eventAsserter.actual()
                .getAuthenticationFailure();
        assertThat(exception).extracting(Throwable::getMessage).asString()
                .contains("Cannot authenticate with unknown or invalid code: wrong-wrong");
    }

}
