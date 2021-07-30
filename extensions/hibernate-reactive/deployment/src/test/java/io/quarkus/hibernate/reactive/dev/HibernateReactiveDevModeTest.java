package io.quarkus.hibernate.reactive.dev;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.response.Response;

/**
 * Checks that public field access is correctly replaced with getter/setter calls,
 * regardless of the field type.
 */
public class HibernateReactiveDevModeTest {

    @RegisterExtension
    static QuarkusDevModeTest runner = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Fruit.class, FruitMutinyResource.class).addAsResource("application.properties")
                    .addAsResource(new StringAsset("INSERT INTO known_fruits(id, name) VALUES (1, 'Cherry');\n" +
                            "INSERT INTO known_fruits(id, name) VALUES (2, 'Apple');\n" +
                            "INSERT INTO known_fruits(id, name) VALUES (3, 'Banana');\n"), "import.sql"));

    @Test
    public void testListAllFruits() {
        Response response = given()
                .when()
                .get("/fruits")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().response();
        assertThat(response.jsonPath().getList("name")).isEqualTo(Arrays.asList("Apple", "Banana", "Cherry"));

        runner.modifySourceFile(Fruit.class, s -> s.replace("ORDER BY f.name", "ORDER BY f.name desk"));
        given()
                .when()
                .get("/fruits")
                .then()
                .statusCode(500);

        runner.modifySourceFile(Fruit.class, s -> s.replace("desk", "desc"));
        response = given()
                .when()
                .get("/fruits")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().response();
        assertThat(response.jsonPath().getList("name")).isEqualTo(Arrays.asList("Cherry", "Banana", "Apple"));
    }
}
