package io.quarkus.security.jpa;

import static io.quarkus.security.jpa.SecurityJpa.jpa;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.time.Duration;
import java.util.Map;

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
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.quarkus.vertx.http.security.Form;
import io.quarkus.vertx.http.security.HttpSecurity;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.smallrye.mutiny.Uni;

class SecurityJpaFluentApiTest {

    private static final String AUGMENTOR_HEADER_NAME = "augmentor-header-name";

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(SingleRoleSecuredServlet.class, MinimalUserEntity.class, SecurityJpaConfiguration.class,
                    FailingIdentityProvider.class)
            .addAsResource("minimal-config/import.sql", "import.sql")
            .addAsResource(new StringAsset("""
                    quarkus.datasource.db-kind=h2
                    quarkus.datasource.username=sa
                    quarkus.datasource.password=sa
                    quarkus.datasource.jdbc.url=jdbc:h2:mem:minimal-config
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
                .header(AUGMENTOR_HEADER_NAME, "form")
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
        RestAssured
                .given()
                .auth().preemptive().basic("user", "user")
                .get("/servlet-secured")
                .then()
                .statusCode(200)
                .header(AUGMENTOR_HEADER_NAME, "basic")
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
                    .build();
            httpSecurity
                    .mechanism(form, jpa().augmentor(new CustomSecurityIdentityAugmentor("form")))
                    .basic(jpa().augmentor(new CustomSecurityIdentityAugmentor("basic")));
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

    private static final class CustomSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

        private final String headerValue;

        private CustomSecurityIdentityAugmentor(String headerValue) {
            this.headerValue = headerValue;
        }

        @Override
        public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
                AuthenticationRequestContext authenticationRequestContext) {
            return augment(securityIdentity, authenticationRequestContext, Map.of());
        }

        @Override
        public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context,
                Map<String, Object> attributes) {
            var routingContext = HttpSecurityUtils.getRoutingContextAttribute(attributes);
            routingContext.response().putHeader(AUGMENTOR_HEADER_NAME, headerValue);
            return Uni.createFrom().item(identity);
        }
    }
}
