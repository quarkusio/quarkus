package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.test.QuarkusUnitTest;

class OpenApiFilterUnknownDocumentNameValidationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClass(FilterWithUnknownDocumentName.class)
                    .addClass(TestResource.class))
            .assertException(throwable -> {
                assertInstanceOf(IllegalArgumentException.class, throwable);
                String message = throwable.getMessage();
                MatcherAssert.assertThat(message, Matchers
                        .is("""
                                Following instances of the OpenAPIFilter annotation are invalid because of a misconfigured documentNames value.
                                Valid values are: [<ALL>, <default>]
                                @OpenAPIFilter 'io.quarkus.smallrye.openapi.test.jaxrs.OpenApiFilterUnknownDocumentNameValidationTest$FilterWithUnknownDocumentName' references unknown document names: [unknown-document]"""));
            });

    @Test
    void testValidation() {
        Assertions.fail("Should have thrown an exception - see OpenApiFilterUnknownDocumentNameValidationTest#runner");
    }

    @Path("/api")
    public static class TestResource {
        @GET
        public String get() {
            return "hello";
        }
    }

    @OpenApiFilter(documentNames = { "unknown-document" })
    public static class FilterWithUnknownDocumentName implements OASFilter {
        @Override
        public void filterOpenAPI(OpenAPI openAPI) {
            openAPI.addExtension("x-test", "value");
        }
    }
}
