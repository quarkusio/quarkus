package io.quarkus.hibernate.orm.dev.livereload;

import static org.hamcrest.Matchers.is;

import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.TestTags;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Test for https://github.com/quarkusio/quarkus/issues/53971
 * Verifies that live reload doesn't fail when entities have validation annotations.
 * The bug was that ClassComparisonUtil used Collectors.toMap() which threw an
 * exception when annotations were repeated, causing live reload to fail.
 */
@Tag(TestTags.DEVMODE)
public class HibernateInstrumentationLiveReloadTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProductEntity.class, ProductResource.class));

    @Test
    public void testLiveReloadWithValidationAnnotations() {
        RestAssured.when().get("/product/test").then().body(is("Product[name=test]"));

        // Change field visibility from public to private - this is the original issue scenario
        TEST.modifySourceFile(ProductEntity.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("public String name;", "private String name;");
            }
        });

        // Should complete without exception - the bug would cause IllegalStateException
        // when compareAnnotations() tried to use toMap() with repeated annotations
        RestAssured.when().get("/product/changed").then().body(is("Product[name=changed]"));
    }
}
