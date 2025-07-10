package io.quarkus.vertx.http.security;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConfigBasedFormAuthCookiesTestCase extends AbstractFormAuthCookiesTestCase {

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
    static QuarkusUnitTest test = createQuarkusApp(APP_PROPS);

}
