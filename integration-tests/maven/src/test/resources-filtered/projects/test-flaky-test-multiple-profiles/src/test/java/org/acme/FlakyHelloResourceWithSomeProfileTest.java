package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@TestProfile(FlakyHelloResourceWithSomeProfileTest.SomeProfile.class)
@QuarkusTest
public class FlakyHelloResourceWithSomeProfileTest {

    @Test
    public void testHelloEndpoint() throws Exception {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("hello"));

        // Should fail the first invocation and pass subsequent ones
        // Use the system classloader to give a consistent view of static variables
        Class flakerClass = ClassLoader.getSystemClassLoader().loadClass(Flaker.class.getName());
        Method flakeMethod = flakerClass.getMethod("flake");
        try {
            flakeMethod.invoke(null);
        } catch (InvocationTargetException e) {
            throw new FlakingException(e.getCause().getMessage());
        }
    }

    public static class SomeProfile implements QuarkusTestProfile {

        public SomeProfile() {
        }
    }

}
