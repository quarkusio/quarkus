package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.given;

import java.util.function.Supplier;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Reproducer for <a href="https://github.com/quarkusio/quarkus/issues/53588">quarkusio/quarkus#53588</a> (point 1):
 * reflection-free Jackson serializers/deserializers ignore the ObjectMapper-level
 * {@code PropertyNamingStrategy} configured via {@code ObjectMapperCustomizer}.
 *
 * <p>
 * This test sets up a global {@code SNAKE_CASE} strategy through {@link SnakeCaseObjectMapperCustomizer}
 * and verifies that a plain record ({@link GlobalNamingRequest}) with camelCase field names
 * serializes/deserializes using snake_case JSON field names.
 *
 * <p>
 * Expected: both tests pass (snake_case names are used in JSON).
 * <br>
 * Actual (bug): the generated reflection-free serializer/deserializer ignores the global strategy
 * and uses the raw Java field names (camelCase), causing assertion failures.
 */
public class GlobalNamingWithReflectionFreeSerializersTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(GlobalNamingRequest.class, GlobalNamingResource.class,
                                    SnakeCaseObjectMapperCustomizer.class)
                            .addAsResource(new StringAsset(
                                    "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            });

    @Test
    void naming_globalSnakeCase_shouldDeserialize() {
        given()
                .contentType("application/json")
                .body("""
                        {"first_name": "Alice", "last_name": "Smith", "age": 30}
                        """)
                .when()
                .post("/global-naming")
                .then()
                .statusCode(200)
                .body("values", CoreMatchers.is("Alice Smith 30"));
    }

    @Test
    void naming_globalSnakeCase_shouldSerialize() {
        given()
                .when()
                .get("/global-naming/ser")
                .then()
                .statusCode(200)
                .body("first_name", CoreMatchers.is("Alice"))
                .body("last_name", CoreMatchers.is("Smith"))
                .body("age", CoreMatchers.is(30));
    }
}
