package io.quarkus.hibernate.validator.test.devmode;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class DevModeConstraintValidationTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestBean.class,
                    DevModeTestResource.class, ClassLevelConstraint.class, ClassLevelValidator.class, DependentTestBean.class));

    @Test
    public void testClassConstraintHotReplacement() {
        RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .body("{}")
                .post("/test/validate")
                .then()
                .body(containsString("ok"));

        TEST.modifySourceFile("TestBean.java",
                s -> s.replace("// <placeholder1>", "@io.quarkus.hibernate.validator.test.devmode.ClassLevelConstraint"));

        RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .body("{}")
                .post("/test/validate")
                .then()
                .body(containsString("My class constraint message"));
    }

    @Test
    public void testPropertyConstraintHotReplacement() {
        RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .body("{}")
                .post("/test/validate")
                .then()
                .body(containsString("ok"));

        TEST.modifySourceFile("TestBean.java", s -> s.replace("// <placeholder2>",
                "@javax.validation.constraints.NotNull(message=\"My property message\")"));

        RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .body("{}")
                .post("/test/validate")
                .then()
                .body(containsString("My property message"));
    }

    @Test
    public void testMethodConstraintHotReplacement() {

        RestAssured.given()
                .when()
                .get("/test/mymessage")
                .then()
                .body(containsString("mymessage"));

        TEST.modifySourceFile("DependentTestBean.java", s -> s.replace("/* <placeholder> */",
                "@javax.validation.constraints.Size(max=1, message=\"My method message\")"));

        RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .get("/test/mymessage")
                .then()
                .body(containsString("My method message"));
    }

    @Test
    public void testNewBeanHotReplacement() {
        RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .body("{}")
                .post("/test/validate")
                .then()
                .body(containsString("ok"));

        TEST.addSourceFile(NewTestBean.class);
        TEST.modifySourceFile("DevModeTestResource.java", s -> s.replace("@Valid TestBean",
                "@Valid NewTestBean"));

        RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .body("{}")
                .post("/test/validate")
                .then()
                .body(containsString("My new bean message"));
    }

    @Test
    public void testNewConstraintHotReplacement() {
        RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .body("{}")
                .post("/test/validate")
                .then()
                .body(containsString("ok"));

        TEST.addSourceFile(NewConstraint.class);
        TEST.addSourceFile(NewValidator.class);
        TEST.modifySourceFile("TestBean.java", s -> s.replace("// <placeholder2>",
                "@NewConstraint"));

        RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .body("{}")
                .post("/test/validate")
                .then()
                .body(containsString("My new constraint message"));
    }
}
