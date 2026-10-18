package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.logging.LogRecord;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.logmanager.Level;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A resource that still uses the legacy {@code javax.ws.rs} annotations is not scanned, so the build must warn about
 * it. The resource class is generated, as the legacy API is not on the classpath.
 */
public class LegacyJaxRsAnnotationsWarningTest {

    private static final String LEGACY_RESOURCE = "com.example.LegacyResource";
    private static final String LOGGER = "io.quarkus.resteasy.reactive.server.deployment.LegacyJaxRsAnnotationsProcessor";

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(HelloResource.class)
                    .add(new ByteArrayAsset(legacyResource()), LEGACY_RESOURCE.replace('.', '/') + ".class"))
            .setLogRecordPredicate(record -> LOGGER.equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                assertThat(records).hasSize(1);
                LogRecord record = records.get(0);
                assertThat(record.getLevel()).isEqualTo(Level.WARN);
                assertThat(record.getMessage()).contains("javax.ws.rs");
                assertThat(record.getParameters()).anySatisfy(p -> assertThat(p.toString()).contains(LEGACY_RESOURCE));
            });

    @Test
    public void legacyResourceIsIgnored() {
        when().get("/hello").then().statusCode(200);
        when().get("/legacy").then().statusCode(404);
    }

    private static byte[] legacyResource() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput((name, data) -> bytes.writeBytes(data))
                .className(LEGACY_RESOURCE)
                .build()) {
            classCreator.addAnnotation("javax.ws.rs.Path").addValue("value", "/legacy");
            MethodCreator methodCreator = classCreator.getMethodCreator("legacy", String.class);
            methodCreator.addAnnotation("javax.ws.rs.GET");
            methodCreator.returnValue(methodCreator.load("legacy"));
        }
        return bytes.toByteArray();
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String hello() {
            return "hello";
        }
    }
}
