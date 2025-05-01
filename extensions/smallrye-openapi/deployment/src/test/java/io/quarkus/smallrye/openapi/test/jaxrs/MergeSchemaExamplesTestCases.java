package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.openapi.api.SmallRyeOASConfig;

abstract class MergeSchemaExamplesTestCases {

    @Schema(name = "Bean")
    static class Bean {
        @Schema(example = "Deprecated example", examples = {
                "New example 1",
                "New example 2"
        })
        private String field;

        public String getField() {
            return field;
        }
    }

    @Path("/resource")
    static class Resource {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Bean get() {
            return new Bean();
        }
    }

    private final String exampleValue;
    private final String[] examplesValue;

    MergeSchemaExamplesTestCases(String exampleValue, String[] examplesValue) {
        this.exampleValue = exampleValue;
        this.examplesValue = examplesValue;
    }

    @Test
    void testExamples() {
        RestAssured.given()
                .header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .log().ifValidationFails()
                .body("components.schemas.Bean.properties.field.example", is(exampleValue))
                .body("components.schemas.Bean.properties.field.examples", contains(examplesValue));
    }

    static class MergeSchemaExamplesDefaultTestCase extends MergeSchemaExamplesTestCases {
        @RegisterExtension
        static QuarkusUnitTest runner = new QuarkusUnitTest()
                .withApplicationRoot((jar) -> jar
                        .addClasses(Resource.class, Bean.class));

        MergeSchemaExamplesDefaultTestCase() {
            super(null, new String[] { "New example 1", "New example 2", "Deprecated example" });
        }
    }

    static class MergeSchemaExamplesQuarkusFalseTestCase extends MergeSchemaExamplesTestCases {
        @RegisterExtension
        static QuarkusUnitTest runner = new QuarkusUnitTest()
                .withApplicationRoot((jar) -> jar
                        .addClasses(Resource.class, Bean.class)
                        .addAsResource(new StringAsset("quarkus.smallrye-openapi.merge-schema-examples=false\n"),
                                "application.properties"));

        MergeSchemaExamplesQuarkusFalseTestCase() {
            super("Deprecated example", new String[] { "New example 1", "New example 2" });
        }
    }

    static class MergeSchemaExamplesSmallRyeFalseTestCase extends MergeSchemaExamplesTestCases {
        @RegisterExtension
        static QuarkusUnitTest runner = new QuarkusUnitTest()
                .withApplicationRoot((jar) -> jar
                        .addClasses(Resource.class, Bean.class)
                        .addAsResource(new StringAsset(SmallRyeOASConfig.SMALLRYE_MERGE_SCHEMA_EXAMPLES + "=false\n"),
                                "application.properties"));

        MergeSchemaExamplesSmallRyeFalseTestCase() {
            super("Deprecated example", new String[] { "New example 1", "New example 2" });
        }
    }
}
