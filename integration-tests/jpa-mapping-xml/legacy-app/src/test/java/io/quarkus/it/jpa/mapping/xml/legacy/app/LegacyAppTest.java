package io.quarkus.it.jpa.mapping.xml.legacy.app;

import static io.restassured.RestAssured.when;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LegacyAppTest {

    /**
     * Tests an annotated entity defined in a library A,
     * whose mapping is overridden in an orm.xml defined in that same library, next to the persistence.xml.
     */
    @Test
    public void libraryAEntitiesAreMapped() {
        when().get("/legacy-app/library-a").then()
                .body(is("OK"))
                .statusCode(200);
    }

    /**
     * Tests an annotated entity defined in a library B,
     * whose mapping is overridden in an orm.xml defined in that same library, next to the persistence.xml.
     * <p>
     * Importantly, the orm.xml file has the same path in the classpath as the orm.xml file of library A,
     * so it requires special care.
     */
    @Test
    public void libraryBEntitiesAreMapped() {
        when().get("/legacy-app/library-b").then()
                .body(is("OK"))
                .statusCode(200);
    }

}
