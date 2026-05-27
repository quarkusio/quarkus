package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

public class PanacheNextProcessorAutoDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "panache-next-processor-auto";
    }

    @Override
    protected void testDevMode() throws Exception {
        // Verify that the /hello endpoint works
        assertThat(getHttpResponse("/hello")).contains("Hello");

        // Verify that the /hello/entity endpoint works, which proves the metamodel was generated
        // This endpoint references MyEntity_.class, which would fail to compile if the
        // hibernate-processor wasn't auto-configured
        assertThat(getHttpResponse("/hello/entity")).contains("Entity metamodel: MyEntity_");

        // Verify live reload: add a new field to the entity and update endpoint to use it
        // Add new field to entity
        replace("src/main/java/org/acme/MyEntity.java",
                Map.of("public int amount;", "public int amount;\n    public String newField;"));

        // Update endpoint to reference the new metamodel field
        replace("src/main/java/org/acme/HelloResource.java",
                Map.of("return \"Entity metamodel: \" + MyEntity_.class.getSimpleName();",
                        "return \"Entity metamodel with field: \" + MyEntity_.NEW_FIELD;"));

        // Wait for recompilation and verify the metamodel was regenerated with the new field
        assertUpdatedResponseContains("/hello/entity", "Entity metamodel with field:");
    }
}
