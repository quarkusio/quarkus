package io.quarkus.it.jpa.mapping.xml.modern.app;

import static io.restassured.RestAssured.when;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests that auto-applied attribute converter mapped using XML only:
 * <ul>
 * <li>are indeed applied automatically</li>
 * <li>have access to the CDI context</li>
 * </ul>
 * <p>
 */
@QuarkusTest
public class XmlMappingOnlyAttributeConverterTest {
    @Test
    public void autoApply() {
        when().get("/xml-mapping-only/attribute-converter/auto-apply").then()
                .body(is("OK"))
                .statusCode(200);
    }
}
