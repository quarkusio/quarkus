package io.quarkus.elytron.security.ldap;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.authentication.FormAuthConfig;
import io.restassured.specification.RequestSpecification;

public class FormAuthTest extends LdapSecurityRealmTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("form-auth/application.properties", "application.properties"));

    @Override
    protected RequestSpecification setupAuth(String username, String password) {
        return RestAssured.given()
                .auth()
                .form(username, password,
                        new FormAuthConfig("j_security_check", "j_username", "j_password")
                                .withLoggingEnabled());
    }

    @Override
    @Override
    protected int getAuthFailureStatusCode() {
        return 302;
    }
}
