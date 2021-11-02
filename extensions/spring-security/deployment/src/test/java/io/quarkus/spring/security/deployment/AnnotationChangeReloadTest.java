package io.quarkus.spring.security.deployment;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.spring.security.deployment.springapp.Person;
import io.quarkus.spring.security.deployment.springapp.Roles;
import io.quarkus.spring.security.deployment.springapp.SpringComponent;
import io.quarkus.spring.security.deployment.springapp.SpringController;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

@Disabled("The spring module does not seem to be included in the classpath for these dev-mode tests")
public class AnnotationChangeReloadTest {

    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Roles.class,
                            Person.class,
                            SpringComponent.class,
                            SpringController.class));

    @Test()
    public void testUpdatedAnnotationWorks() {
        RestAssured.given().auth().preemptive().basic("bob", "bob")
                .when().get("/secure/admin").then()
                .statusCode(403);
        RestAssured.given().auth().preemptive().basic("alice", "alice")
                .when().get("/secure/admin").then()
                .statusCode(200);

        TEST.modifySourceFile("SpringComponent.java", s -> s.replace("@PreAuthorize(\"hasRole(@roles.ADMIN)\")",
                "@PreAuthorize(\"hasRole(@roles.USER)\")"));

        RestAssured.given().auth().preemptive().basic("bob", "bob")
                .when().get("/secure/admin").then()
                .statusCode(200);
        RestAssured.given().auth().preemptive().basic("alice", "alice")
                .when().get("/secure/admin").then()
                .statusCode(403);
    }
}
