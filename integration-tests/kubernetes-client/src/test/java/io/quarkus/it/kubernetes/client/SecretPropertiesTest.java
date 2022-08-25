package io.quarkus.it.kubernetes.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTestResource(value = CustomKubernetesMockServerTestResource.class, restrictToAnnotatedClass = true)
@QuarkusTest
public class SecretPropertiesTest {

    @Test
    public void testPropertiesReadFromConfigMap() {
        assertProperty("dummysecret", "dummysecret");
        assertProperty("overriddensecret", "secret");
        assertProperty("s1SecretProp1", "val1");
        assertProperty("s2SecretProp1", "val99");
        assertProperty("secretProp2", "val2");
        assertProperty("secretProp3", "val3");
        assertProperty("secretProp4", "val4");
    }

    public static void assertProperty(String propertyName, String expectedValue) {
        given()
                .when().get("/secretProperties/" + propertyName)
                .then()
                .statusCode(200)
                .body(is(expectedValue));
    }
}
