package io.quarkus.it.jpa.mapping.xml.modern.app;

import static io.restassured.RestAssured.when;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ModernAppTest {

    /**
     * Tests an annotated entity defined in a library A,
     * whose mapping is overridden in an orm.xml defined in that same library,
     * with a uniquely identifying name referenced in application.properties.
     */
    @Test
    public void libraryAEntitiesAreMapped() {
        when().get("/modern-app/library-a").then()
                .body(is("OK"))
                .statusCode(200);
    }

    /**
     * Tests an annotated entity defined in a library B,
     * whose mapping is overridden in an orm.xml defined in that same library,
     * with a uniquely identifying name referenced in application.properties.
     * <p>
     * Importantly, the orm.xml file has a different name from the one in library A.
     */
    @Test
    public void libraryBEntitiesAreMapped() {
        when().get("/modern-app/library-b").then()
                .body(is("OK"))
                .statusCode(200);
    }
}
