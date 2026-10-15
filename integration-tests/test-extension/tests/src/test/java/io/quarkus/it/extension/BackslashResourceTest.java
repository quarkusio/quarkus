package io.quarkus.it.extension;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests glob pattern resource inclusion in native mode.
 * Only Windows-safe filenames are used, e.g. * and \ are illegal in
 * Windows filenames as per
 * <a href="https://learn.microsoft.com/en-us/windows/win32/fileio/naming-a-file">Win32 naming-a-file</a>
 */
@QuarkusTest
public class BackslashResourceTest {

    @Test
    public void testGlobstarResourceAtRoot() {
        when()
                .get("/core/glob-resource?path=globstar/test.myfiles")
                .then()
                .body(is("globstar test content"));
    }

    @Test
    public void testGlobstarResourceNested() {
        when()
                .get("/core/glob-resource?path=globstar/level1/level2/nested.myfiles")
                .then()
                .body(is("globstar nested test"));
    }

    /**
     * Tests nested wildcard: config/&#42;/settings/&#42;.properties
     * <p>
     * i.e. config/{ANY_DIR}/settings/{ANY_FILE}.properties
     */
    @Test
    public void testComplexWildcardPatternNest() {
        when()
                .get("/core/glob-resource?path=config/app/settings/X.properties")
                .then()
                .body(is("ABAB666"));
    }

    @Test
    public void testComplexWildcardPatternUnderscore() {
        when()
                .get("/core/glob-resource?path=config/app/settings/_properties.properties")
                .then()
                .body(is("XYZ123"));
    }

    @Test
    public void testComplexWildcardPatternTilda() {
        when()
                .get("/core/glob-resource?path=config/db/settings/~.properties")
                .then()
                .body(is("TEST1234"));
    }
}
