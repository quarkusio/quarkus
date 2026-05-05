package io.quarkus.email.authentication.deployment.test;

import static io.quarkus.email.authentication.deployment.test.TestEmailAuthenticationHelper.extractCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

class EmailAuthenticationDevModeTest {

    private static final String EMAIL_ADDRESS = "test-vaclav@quarkus.io";

    private static final TestEmailAuthenticationHelper HELPER = new TestEmailAuthenticationHelper(
            "/generate-email-authentication-code", "/j_security_check", "Your verification code", "Your verification code is",
            "email", "code");

    @RegisterExtension
    static final QuarkusDevModeTest APP = new QuarkusDevModeTest()
            .withApplicationRoot(HELPER.getAppConfig(CodeProvider.class));

    @Test
    void testEncryptionKeyPersistedAmongLiveReloads() {
        // go to secured page and expect redirection to login page where we can submit username
        String targetPath = "/secured/admin";
        HELPER.goTo(targetPath).statusCode(302).header("location", containsString("/login.html"));
        HELPER.assertCookie("quarkus-redirect-location", targetPath);

        // request code and expect redirection to a form where we can submit code
        HELPER.requestCodeFor(EMAIL_ADDRESS).statusCode(302).header("location", containsString("/code.html"));
        Awaitility.await().untilAsserted(
                () -> RestAssured.given().get("/get-code").then().statusCode(200).body(not(blankOrNullString())));
        String code = extractCode(RestAssured.given().get("/get-code").then().statusCode(200).extract().asString().trim());
        assertThat(code).hasSize(15);

        // login with code and then get session cookie
        HELPER.submitCode(code).statusCode(302).header("location", containsString(targetPath));
        HELPER.assertCookieMissing("quarkus-credential-request");
        HELPER.assertCookieMissing("quarkus-redirect-location");
        HELPER.assertCookiePresent("quarkus-credential");

        // use the session cookie to access a path that requires 'admin' role
        HELPER.goTo(targetPath).statusCode(200).body(is(EMAIL_ADDRESS + ":" + targetPath));

        // change some property so that the app is forced to restart
        APP.modifyResourceFile("application.properties",
                props -> props + System.lineSeparator() + "quarkus.email-authentication.email-parameter=changed");

        // if our session cookie still works, it means that the encryption code didn't change
        HELPER.goTo(targetPath).statusCode(200).body(is(EMAIL_ADDRESS + ":" + targetPath));

        // now change the encryption key and expect that the session cookie is invalid
        APP.modifyResourceFile("application.properties",
                props -> props + System.lineSeparator() + "quarkus.http.auth.session.encryption-key=1234567890ABCDELFGH");

        // if our session cookie still works, it means that the encryption code didn't change
        HELPER.goTo(targetPath).statusCode(302).header("location", containsString("/login.html"));
    }

    static class CodeProvider {

        void exposeCodeEndpoint(@Observes Router router, Instance<MockMailbox> mockMailbox) {
            TestEmailIdentityAugmentor.addUser(EMAIL_ADDRESS, "admin");
            router.get("/get-code").order(1).handler(ctx -> {
                var mails = mockMailbox.get().getMailsSentTo(EMAIL_ADDRESS);
                if (mails == null || mails.isEmpty()) {
                    ctx.end("");
                } else {
                    ctx.end(mails.get(0).getText());
                }
            });
        }

    }
}
