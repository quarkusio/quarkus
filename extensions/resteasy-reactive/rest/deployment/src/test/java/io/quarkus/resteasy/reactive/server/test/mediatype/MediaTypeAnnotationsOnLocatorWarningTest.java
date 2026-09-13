package io.quarkus.resteasy.reactive.server.test.mediatype;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.logging.Level;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * {@code @Consumes} and {@code @Produces} declared on a sub-resource locator do not apply to the sub-resource
 * methods, so the build logs a warning for them.
 */
public class MediaTypeAnnotationsOnLocatorWarningTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(AnnotatedLocatorResource.class, ClassLevelProducesResource.class, SubResource.class))
            .setLogRecordPredicate(record -> record.getLevel().equals(Level.WARNING)
                    && record.getMessage().contains("sub-resource locator"))
            .assertLogRecords(records -> {
                assertThat(records).map(WarningMessages::format).hasSize(2);
                assertThat(records).map(WarningMessages::format)
                        .anyMatch(m -> m.contains("Method 'producesLocator'") && m.contains("@Produces"))
                        .anyMatch(m -> m.contains("Method 'consumesLocator'") && m.contains("@Consumes"))
                        .noneMatch(m -> m.contains("ClassLevelProducesResource"));
            });

    @Test
    public void subResourcesStillWork() {
        get("/annotated/produces/sub").then().statusCode(200).body(equalTo("sub"));
        get("/annotated/consumes/sub").then().statusCode(200).body(equalTo("sub"));
        get("/class-level/sub").then().statusCode(200).body(equalTo("sub"));
    }

    @Path("annotated")
    public static class AnnotatedLocatorResource {

        @Path("produces")
        @Produces(MediaType.APPLICATION_JSON)
        public SubResource producesLocator() {
            return new SubResource();
        }

        @Path("consumes")
        @Consumes(MediaType.APPLICATION_JSON)
        public SubResource consumesLocator() {
            return new SubResource();
        }
    }

    @Path("class-level")
    @Produces(MediaType.TEXT_PLAIN)
    public static class ClassLevelProducesResource {

        @Path("sub")
        public SubResource locator() {
            return new SubResource();
        }
    }

    public static class SubResource {

        @GET
        @Path("sub")
        @Produces(MediaType.TEXT_PLAIN)
        public String sub() {
            return "sub";
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String root() {
            return "sub";
        }
    }
}
