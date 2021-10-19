package io.quarkus.resteasy.reactive.server.test.generatedresource;

import static io.restassured.RestAssured.when;

import java.util.function.Consumer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.resteasy.reactive.spi.GeneratedJaxRsResourceGizmoAdaptor;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.resteasy.reactive.spi.GeneratedJaxRsResourceBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class GeneratedJaxRsResourceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(HelloResource.class))
            .addBuildChainCustomizer(buildCustomizer());

    @Test
    public void testRestPath() {
        when().get("/hello").then().statusCode(200).body(Matchers.is("hello"));
        when().get("/test").then().statusCode(200).body(Matchers.is("test"));
    }

    protected static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<>() {
            /**
             * This represents the extension that generates a JAX-RS resource like:
             *
             * <pre>
             * {@code
             *      &#64;Path("/test')
             *      public class TestResource {
             *
             *          &#64;GET
             *          public String test() {
             *              return "test";
             *          }
             *      }
             * }
             * </pre>
             */
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(context -> {
                    BuildProducer<GeneratedJaxRsResourceBuildItem> producer = context::produce;
                    ClassOutput classOutput = new GeneratedJaxRsResourceGizmoAdaptor(producer);
                    try (ClassCreator classCreator = ClassCreator.builder()
                            .classOutput(classOutput).className("com.example.TestResource")
                            .build()) {
                        classCreator.addAnnotation(Path.class).addValue("value", "test");
                        MethodCreator methodCreator = classCreator.getMethodCreator("test", String.class);
                        methodCreator.addAnnotation(GET.class);
                        methodCreator.returnValue(methodCreator.load("test"));
                    }
                }).produces(GeneratedJaxRsResourceBuildItem.class).build();
            }
        };
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String hello() {
            return "hello";
        }
    }
}
