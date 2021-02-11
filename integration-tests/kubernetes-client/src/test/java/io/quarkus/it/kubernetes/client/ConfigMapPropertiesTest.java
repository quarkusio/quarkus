package io.quarkus.it.kubernetes.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTestResource(value = CustomKubernetesMockServerTestResource.class, restrictToAnnotatedClass = true)
@QuarkusTest
public class ConfigMapPropertiesTest {

    @Test
    public void testPropertiesReadFromConfigMap() {
        assertProperty("dummy", "dummy");
        assertProperty("someProp1", "val1");
        assertProperty("someProp2", "val2");
        assertProperty("someProp3", "val3");
        assertProperty("someProp4", "val4");
        assertProperty("someProp5", "val5");
    }

    public static void assertProperty(String propertyName, String expectedValue) {
        given()
                .when().get("/configMapProperties/" + propertyName)
                .then()
                .statusCode(200)
                .body(is(expectedValue));
    }

}
