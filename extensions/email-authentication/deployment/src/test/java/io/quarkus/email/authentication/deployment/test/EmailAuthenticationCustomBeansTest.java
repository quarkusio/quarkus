package io.quarkus.email.authentication.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.email.authentication.EmailAuthenticationCodeSender;
import io.quarkus.email.authentication.EmailAuthenticationCodeStorage;
import io.quarkus.email.authentication.EmailAuthenticationRequest;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

class EmailAuthenticationCustomBeansTest {

    private static final TestEmailAuthenticationHelper HELPER = new TestEmailAuthenticationHelper(
            "/generate-email-authentication-code", "/j_security_check", "Your verification code", "Your verification code is",
            "email", "code");

    @RegisterExtension
    static final QuarkusUnitTest APP = new QuarkusUnitTest()
            .withApplicationRoot(HELPER.getAppConfig(CustomSender.class, CustomStorage.class, CustomIdentityProvider.class));

    @Inject
    CustomStorage storage;

    @Inject
    CustomSender sender;

    @BeforeEach
    void reset() {
        HELPER.clear();
        sender.emailToCode.clear();
        storage.emailToCode.clear();
        storage.usedCodes.clear();
        storage.emailToNumOfReq.clear();
    }

    @Test
    void testLoginSuccess() {
        String emailAddress = "test-vaclav@quarkus.io";
        TestEmailIdentityAugmentor.addUser("custom-" + emailAddress, "admin");

        // go to secured page and expect redirection to login page where we can submit username
        String targetPath = "/secured/admin";
        HELPER.goTo(targetPath).statusCode(302).header("location", containsString("/login.html"));
        HELPER.assertCookie("quarkus-redirect-location", targetPath);

        HELPER.requestCodeFor(emailAddress).statusCode(302).header("location", containsString("/code.html"));
        String code = getAndAssertCode(emailAddress);

        // login with code and then get session cookie
        HELPER.submitCode(code).statusCode(302).header("location", containsString(targetPath));
        HELPER.assertCookiePresent("quarkus-credential");

        // use the session cookie to access a path that requires 'admin' role
        HELPER.goTo(targetPath).statusCode(200).body(is("custom-" + emailAddress + ":" + targetPath));
    }

    @Test
    void testPreventReuse() {
        String email = "Erik@quarkus.io";
        HELPER.requestCodeFor(email).statusCode(302).header("location", containsString("/code.html"));
        String code = getAndAssertCode(email);

        // login with code and then get session cookie
        HELPER.submitCode(code).statusCode(302).header("location", containsString("/index.html"));
        HELPER.assertCookiePresent("quarkus-credential");
        HELPER.clear();

        // now try to reuse the code
        HELPER.submitCode(code).statusCode(302).header("location", containsString("/error.html"));
        HELPER.assertCookieMissing("quarkus-credential");
    }

    @Test
    void testPreventTooManyRequests() {
        String email = "Steven@quarkus.io";

        // request number one -> allowed
        HELPER.requestCodeFor(email).statusCode(302).header("location", containsString("/code.html"));
        getAndAssertCode(email);

        // request number two -> allowed
        HELPER.requestCodeFor(email).statusCode(302).header("location", containsString("/code.html"));
        getAndAssertCode(email);

        // request number three -> allowed
        HELPER.requestCodeFor(email).statusCode(302).header("location", containsString("/code.html"));
        getAndAssertCode(email);

        // request number four -> allowed
        HELPER.requestCodeFor(email).statusCode(302).header("location", containsString("/code.html"));
        getAndAssertCode(email);

        // request number five -> not allowed, the limit is set to 5
        HELPER.requestCodeFor(email).statusCode(302).header("location", containsString("/error.html"));
    }

    private String getAndAssertCode(String username) {
        Awaitility.await().until(() -> sender.emailToCode.containsKey(username));
        Awaitility.await().until(() -> storage.emailToCode.containsKey(username));
        assertThat(sender.emailToCode).hasSize(1);
        assertThat(storage.emailToCode).hasSize(1);
        assertThat(sender.emailToCode.get(username)).isEqualTo(storage.emailToCode.get(username));
        return storage.emailToCode.get(username);
    }

    @Singleton
    static class CustomSender implements EmailAuthenticationCodeSender {

        private final Map<String, String> emailToCode = new ConcurrentHashMap<>();

        @Override
        public Uni<Void> sendCode(char[] code, String email) {
            emailToCode.put(email, String.valueOf(code));
            return Uni.createFrom().voidItem();
        }
    }

    @Singleton
    static class CustomStorage implements EmailAuthenticationCodeStorage {

        private final Map<String, String> emailToCode = new ConcurrentHashMap<>();
        private final Set<String> usedCodes = ConcurrentHashMap.newKeySet();
        private final Map<String, AtomicInteger> emailToNumOfReq = new ConcurrentHashMap<>();
        private final EmailAuthenticationCodeStorage delegate; // just so that we know it can be used as a delegate

        CustomStorage(DefaultEmailAuthenticationCodeStorage delegate) {
            this.delegate = delegate;
        }

        @Override
        public Uni<Void> storeCode(EmailAuthenticationCodeRequest req, String emailAddress, RoutingContext event) {
            int regNum = emailToNumOfReq.computeIfAbsent(emailAddress, k -> new AtomicInteger()).incrementAndGet();
            if (regNum >= 5) {
                return Uni.createFrom()
                        .failure(new AuthenticationFailedException("Too many requests for email address " + emailAddress));
            }

            emailToCode.put(emailAddress, String.valueOf(req.code()));
            return delegate.storeCode(req, emailAddress, event);
        }

        @Override
        public Uni<String> findEmailAddressByCode(String code, RoutingContext routingContext) {
            if (usedCodes.contains(code)) {
                return Uni.createFrom().nullItem();
            }

            usedCodes.add(code);
            for (var e : emailToCode.entrySet()) {
                String expectedCode = e.getValue();
                if (expectedCode.equals(code)) {
                    return Uni.createFrom().item(e.getKey());
                }
            }
            return delegate.findEmailAddressByCode(code, routingContext);
        }
    }

    @ApplicationScoped
    static class CustomIdentityProvider implements IdentityProvider<EmailAuthenticationRequest> {

        @Override
        public Class<EmailAuthenticationRequest> getRequestType() {
            return EmailAuthenticationRequest.class;
        }

        @Override
        public Uni<SecurityIdentity> authenticate(EmailAuthenticationRequest req, AuthenticationRequestContext reqCtx) {
            final String principalName;
            if (req.getEmailAddress().startsWith("custom-")) {
                principalName = req.getEmailAddress();
            } else {
                principalName = "custom-" + req.getEmailAddress();
            }
            return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
                    .setPrincipal(new QuarkusPrincipal(principalName))
                    .build());
        }
    }
}
