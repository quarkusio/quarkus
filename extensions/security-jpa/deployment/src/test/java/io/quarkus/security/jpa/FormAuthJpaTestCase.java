package io.quarkus.security.jpa;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;

public class FormAuthJpaTestCase {

    private static final String APP_PROPS = "" +
            "quarkus.datasource.db-kind=h2\n" +
            "quarkus.datasource.username=sa\n" +
            "quarkus.datasource.password=sa\n" +
            "quarkus.datasource.jdbc.url=jdbc:h2:mem:minimal-config'\n" +
            "quarkus.hibernate-orm.sql-load-script=import.sql\n" +
            "quarkus.hibernate-orm.database.generation=drop-and-create\n" +
            "#quarkus.hibernate-orm.log.sql=true\n" +
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
            "quarkus.http.auth.session.encryption-key=CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT-CHANGEIT\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SingleRoleSecuredServlet.class, TestApplication.class, RolesEndpointClassLevel.class,
                            ParametrizedPathsResource.class, SubjectExposingResource.class, MinimalUserEntity.class)
                    .addAsResource("minimal-config/import.sql", "import.sql")
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @Test
    public void testFormBasedAuthSuccess() {
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

}
