package io.quarkus.it.jpa.mapping.xml.modern.app;

import static io.restassured.RestAssured.when;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests that entity listeners mapped using XML only:
 * <ul>
 * <li>are actually used</li>
 * <li>have access to the CDI context</li>
 * </ul>
 * <p>
 */
@QuarkusTest
public class XmlMappingOnlyEntityListenerTest {
    @Test
    public void entityListenersAnnotation() {
        when().get("/xml-mapping-only/entity-listener/entity-listeners-annotation").then()
                .body(is("OK"))
                .statusCode(200);
    }

    @Test
    public void entityInstanceMethods() {
        when().get("/xml-mapping-only/entity-listener/entity-instance-methods").then()
                .body(is("OK"))
                .statusCode(200);
    }
}
