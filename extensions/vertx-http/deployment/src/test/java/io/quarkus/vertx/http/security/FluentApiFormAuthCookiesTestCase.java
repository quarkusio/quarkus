package io.quarkus.vertx.http.security;

import java.time.Duration;

import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.FormAuthConfig.CookieSameSite;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;

public class FluentApiFormAuthCookiesTestCase extends AbstractFormAuthCookiesTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = createQuarkusApp("", HttpSecurityConfigurator.class);

    public static class HttpSecurityConfigurator {

        void configureHttpPermission(@Observes HttpSecurity httpSecurity) {
            httpSecurity.path("/admin%E2%9D%A4").roles("admin");
        }

        void configureFormAuthentication(@Observes HttpSecurity httpSecurity) {
            HttpAuthenticationMechanism formMechanism = Form.builder()
                    .loginPage("login")
                    .errorPage("error")
                    .landingPage("landing")
                    .timeout(Duration.ofSeconds(2))
                    .newCookieInterval(Duration.ofSeconds(1))
                    .cookieName("laitnederc-sukrauq")
                    .cookieSameSite(CookieSameSite.LAX)
                    .httpOnlyCookie()
                    .cookieMaxAge(Duration.ofMinutes(2))
                    .encryptionKey("CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT")
                    .build();
            httpSecurity.mechanism(formMechanism);
        }
    }
}
