package io.quarkus.security.test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;

import java.nio.file.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests of BASIC authentication mechanism
 */
public class BasicAuthTestCase {

    @TempDir
    static Path tempDir;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestSecureServlet.class, TestApplication.class,
                            RolesEndpointClassLevel.class, ParametrizedPathsResource.class,
                            SubjectExposingResource.class)
                    .addAsResource(new StringAsset(
                            "quarkus.security.filesystem.path=" + tempDir.toString().replace("\\", "\\\\")),
                            "application.properties"));

    @BeforeAll
    static void registerUsers() throws Exception {
        FilesystemHelper helper = new FilesystemHelper(tempDir);
        helper.addUser("scott", "jb0ss", "Admin", "admin", "Tester", "user");
        helper.addUser("jdoe", "p4ssw0rd", "NoRolesUser");
        helper.addUser("stuart", "test", "admin", "user");
        helper.addUser("noadmin", "n0Adm1n", "user");
        helper.addUser("alice", "alice");
    }

    // Basic @ServletSecurity tests
    @Test()
    public void testSecureAccessFailure() {
        when().get("/secure-test").then()
                .statusCode(401);
    }

    @Test()
    public void testSecureRoleFailure() {
        given().auth().preemptive().basic("jdoe", "p4ssw0rd")
                .when().get("/secure-test").then()
                .statusCode(403);
    }

    @Test()
    public void testSecureAccessSuccess() {
        given().auth().preemptive().basic("stuart", "test")
                .when().get("/secure-test").then()
                .statusCode(200);
    }

    /**
     * Test access a secured jaxrs resource without any authentication. should see 401 error code.
     */
    @Test
    public void testJaxrsGetFailure() {
        when().get("/jaxrs-secured/rolesClass").then()
                .header("www-authenticate", containsStringIgnoringCase("basic"))
                .statusCode(401);
    }

    /**
     * Test access a secured jaxrs resource with authentication, but no authorization. should see 403 error code.
     */
    @Test
    public void testJaxrsGetRoleFailure() {
        given().auth().preemptive().basic("jdoe", "p4ssw0rd")
                .when().get("/jaxrs-secured/rolesClass").then()
                .statusCode(403);
    }

    /**
     * Test access a secured jaxrs resource with authentication, and authorization. should see 200 success code.
     */
    @Test
    public void testJaxrsGetRoleSuccess() {
        given().auth().preemptive().basic("scott", "jb0ss")
                .when().get("/jaxrs-secured/rolesClass").then()
                .statusCode(200);
    }

    /**
     * Test access a secured jaxrs resource with authentication, and authorization. should see 200 success code.
     */
    @Test
    public void testJaxrsPathAdminRoleSuccess() {
        given().auth().preemptive().basic("scott", "jb0ss")
                .when().get("/jaxrs-secured/parameterized-paths/my/banking/admin").then()
                .statusCode(200);
    }

    @Test
    public void testJaxrsPathAdminRoleFailure() {
        given().auth().preemptive().basic("noadmin", "n0Adm1n")
                .when().get("/jaxrs-secured/parameterized-paths/my/banking/admin").then()
                .statusCode(403);
    }

    /**
     * Test access a secured jaxrs resource with authentication, and authorization. should see 200 success code.
     */
    @Test
    public void testJaxrsPathUserRoleSuccess() {
        given().auth().preemptive().basic("stuart", "test")
                .when().get("/jaxrs-secured/parameterized-paths/my/banking/view").then()
                .statusCode(200);
    }

    /**
     * Test access a secured jaxrs resource with authentication, and authorization. should see 200 success code.
     */
    @Test
    public void testJaxrsUserRoleSuccess() {
        given().auth().preemptive().basic("scott", "jb0ss")
                .when().get("/jaxrs-secured/subject/secured").then()
                .statusCode(200)
                .body(equalTo("scott"));
    }

    @Test
    public void testJaxrsInjectedPrincipalSuccess() {
        given().auth().preemptive().basic("scott", "jb0ss")
                .when().get("/jaxrs-secured/subject/principalSecured").then()
                .statusCode(200)
                .body(equalTo("scott"));
    }

    /**
     * Test access a @PermitAll secured jaxrs resource without any authentication. should see a 200 success code.
     */
    @Test
    public void testJaxrsGetPermitAll() {
        when().get("/jaxrs-secured/subject/unsecured").then()
                .statusCode(200)
                .body(equalTo("anonymous"));
    }

    /**
     * Test access a @DenyAll secured jaxrs resource without authentication. should see a 401 success code.
     */
    @Test
    public void testJaxrsGetDenyAllWithoutAuth() {
        when().get("/jaxrs-secured/subject/denied").then()
                .statusCode(401);
    }

    /**
     * Test access a @DenyAll secured jaxrs resource with authentication. should see a 403 success code.
     */
    @Test
    public void testJaxrsGetDenyAllWithAuth() {
        given().auth().preemptive().basic("scott", "jb0ss")
                .when().get("/jaxrs-secured/subject/denied").then()
                .statusCode(403);
    }
}
