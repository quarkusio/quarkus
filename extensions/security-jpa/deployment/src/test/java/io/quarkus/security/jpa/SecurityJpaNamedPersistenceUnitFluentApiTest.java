package io.quarkus.security.jpa;

import static io.quarkus.security.jpa.SecurityJpa.jpa;
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
import io.quarkus.security.jpa.other.OtherMinimalUserEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.security.Form;
import io.quarkus.vertx.http.security.HttpSecurity;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.smallrye.mutiny.Uni;

class SecurityJpaNamedPersistenceUnitFluentApiTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(SingleRoleSecuredServlet.class, OtherMinimalUserEntity.class, SecurityJpaConfiguration.class,
                    FailingIdentityProvider.class)
            .addAsResource("minimal-config/import.sql", "import.sql")
            .addAsResource("minimal-config/import-one-user.sql", "import-one-user.sql")
            .addAsResource(new StringAsset("""
                    quarkus.datasource.named.db-kind=h2
                    quarkus.datasource.named.username=sa
                    quarkus.datasource.named.password=sa
                    quarkus.datasource.named.jdbc.url=jdbc:h2:mem:minimal-config-1
                    quarkus.hibernate-orm.named.sql-load-script=import.sql
                    quarkus.hibernate-orm.named.schema-management.strategy=drop-and-create
                    quarkus.hibernate-orm.named.packages=io.quarkus.security.jpa.other
                    quarkus.hibernate-orm.named.datasource=named
                    quarkus.datasource.other-named.db-kind=h2
                    quarkus.datasource.other-named.username=sa
                    quarkus.datasource.other-named.password=sa
                    quarkus.datasource.other-named.jdbc.url=jdbc:h2:mem:minimal-config-2
                    quarkus.hibernate-orm.other-named.sql-load-script=import-one-user.sql
                    quarkus.hibernate-orm.other-named.schema-management.strategy=drop-and-create
                    quarkus.hibernate-orm.other-named.packages=io.quarkus.security.jpa.other
                    quarkus.hibernate-orm.other-named.datasource=other-named
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
                .get("/servlet-secured")
                .then()
                .assertThat()
                .statusCode(302)
                .header("location", containsString("/login"))
                .cookie("quarkus-redirect-location", containsString("/servlet-secured"));

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
                .header("location", containsString("/servlet-secured"))
                .cookie("laitnederc-sukrauq", notNullValue());

        RestAssured
                .given()
                .filter(cookies)
                .redirects().follow(false)
                .when()
                .get("/servlet-secured")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("A secured message"));
    }

    @Test
    void testBasicAuthentication() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured
                .given()
                .auth().preemptive().basic("user", "wrong-password")
                .get("/servlet-secured")
                .then()
                .statusCode(401);
        // these are correct credentials for persistence unit 'named', but not for the persistence unit 'other-named'
        // and the credentials worked for the form-authentication in the other test method of this class
        RestAssured
                .given()
                .auth().preemptive().basic("user", "user")
                .get("/servlet-secured")
                .then()
                .statusCode(401);
        // now use correct credentials for persistence unit 'other-named'
        RestAssured
                .given()
                .auth().preemptive().basic("robin", "robin")
                .get("/servlet-secured")
                .then()
                .statusCode(200)
                .body(equalTo("A secured message"));
    }

    static class SecurityJpaConfiguration {

        void configure(@Observes HttpSecurity httpSecurity) {
            var form = Form.builder()
                    .loginPage("login")
                    .errorPage("error")
                    .landingPage("landing")
                    .cookieName("laitnederc-sukrauq")
                    .newCookieInterval(Duration.ofSeconds(5))
                    .timeout(Duration.ofSeconds(5))
                    .encryptionKey("CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT")
                    .identityProviders(jpa("named"), jpaTrustedProvider("named"))
                    .build();
            httpSecurity
                    .mechanism(form)
                    .basic(jpa("other-named"));
        }

    }

    @ApplicationScoped
    static class FailingIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

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
