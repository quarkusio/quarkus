package io.quarkus.security.jpa.reactive;

import static io.quarkus.security.jpa.SecurityJpa.jpaTrustedProvider;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.jpa.SecurityJpa;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.security.Form;
import io.quarkus.vertx.http.security.HttpSecurity;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.smallrye.mutiny.Uni;

class SecurityJpaReactiveFluentApiTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(TestApplication.class, MinimalUserEntity.class, SecurityJpaConfiguration.class,
                    SingleRoleSecuredResource.class, FailingIdentityProvider.class)
            .addAsResource("minimal-config/import.sql", "import.sql")
            .addAsResource(new StringAsset("""
                    quarkus.datasource.db-kind=postgresql
                    quarkus.datasource.username=${postgres.reactive.username}
                    quarkus.datasource.password=${postgres.reactive.password}
                    quarkus.datasource.reactive=true
                    quarkus.datasource.reactive.url=${postgres.reactive.url}
                    quarkus.hibernate-orm.sql-load-script=import.sql
                    quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                    """), "application.properties"));

    @Test
    void testFormBasedAuthentication() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/jaxrs-secured/user-secured")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/login"))
                .cookie("quarkus-redirect-location", containsString("/user-secured"));

        // test with a non-existent user
        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "dummy")
                .formParam("j_password", "dummy")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302);

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .formParam("j_username", "user")
                .formParam("j_password", "user")
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/user-secured"))
                .cookie("laitnederc-sukrauq", notNullValue());

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/jaxrs-secured/user-secured")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("A secured message"));
    }

    @Test
    void testBasicAuthentication() {
        RestAssured
                .given()
                .auth().preemptive().basic("user", "wrong-password")
                .get("/jaxrs-secured/user-secured")
                .then()
                .statusCode(401);
        RestAssured
                .given()
                .auth().preemptive().basic("user", "user")
                .get("/jaxrs-secured/user-secured")
                .then()
                .statusCode(200)
                .body(equalTo("A secured message"));
    }

    public static class SecurityJpaConfiguration {

        void configure(@Observes HttpSecurity httpSecurity) {
            var jpa = SecurityJpa.jpa();
            var form = Form.builder()
                    .loginPage("login")
                    .errorPage("error")
                    .landingPage("landing")
                    .cookieName("laitnederc-sukrauq")
                    .newCookieInterval(Duration.ofSeconds(5))
                    .timeout(Duration.ofSeconds(5))
                    .encryptionKey("CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT")
                    .identityProviders(jpa, jpaTrustedProvider())
                    .build();
            httpSecurity.mechanism(form).basic(jpa);
        }

    }

    @ApplicationScoped
    public static class FailingIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

        @Override
        public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
            return UsernamePasswordAuthenticationRequest.class;
        }

        @Override
        public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest authenticationRequest,
                AuthenticationRequestContext authenticationRequestContext) {
            throw new IllegalStateException("This provider must never be invoked as we selected the JPA provider");
        }

        @Override
        public int priority() {
            return Integer.MAX_VALUE;
        }
    }
}
