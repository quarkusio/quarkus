package io.quarkus.it.jpa.mapping.xml.modern.app;

import static io.restassured.RestAssured.when;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests dirty checking for entities mapped using XML only.
 * <p>
 * This is important because bytecode enhancement relies on annotations exclusively,
 * so it's not going to be applied to entities mapped using XML only,
 * and we will fall back to the "traditional" way of detecting dirty entities.
 */
@QuarkusTest
public class XmlMappingOnlyDirtyCheckingTest {

    @Test
    public void basic() {
        when().get("/xml-mapping-only/dirty-checking/basic").then()
                .body(is("OK"))
                .statusCode(200);
    }

    @Test
    public void embedded_replace() {
        when().get("/xml-mapping-only/dirty-checking/embedded/replace").then()
                .body(is("OK"))
                .statusCode(200);
    }

    @Test
    public void embedded_update() {
        when().get("/xml-mapping-only/dirty-checking/embedded/update").then()
                .body(is("OK"))
                .statusCode(200);
    }

    @Test
    public void elementCollection() {
        when().get("/xml-mapping-only/dirty-checking/element-collection").then()
                .body(is("OK"))
                .statusCode(200);
    }

    @Test
    public void oneToOne() {
        when().get("/xml-mapping-only/dirty-checking/one-to-one").then()
                .body(is("OK"))
                .statusCode(200);
    }

    @Test
    public void manyToOne() {
        when().get("/xml-mapping-only/dirty-checking/many-to-one").then()
                .body(is("OK"))
                .statusCode(200);
    }

    @Test
    public void oneToMany() {
        when().get("/xml-mapping-only/dirty-checking/one-to-many").then()
                .body(is("OK"))
                .statusCode(200);
    }

    @Test
    public void manyToMany() {
        when().get("/xml-mapping-only/dirty-checking/many-to-many").then()
                .body(is("OK"))
                .statusCode(200);
    }
}
