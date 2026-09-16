package io.quarkus.resteasy.reactive.server.test.beanparam;

import static org.hamcrest.Matchers.equalTo;

import java.util.function.Consumer;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.resteasy.reactive.server.spi.AnnotationsTransformerBuildItem;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * The parameter annotations of the bean parameter classes are not in the source: an annotation transformer adds them.
 */
public class BeanParamWithTransformedAnnotationsTest {

    private static final DotName FORM_COMMAND = DotName.createSimple(FormCommand.class.getName());
    private static final DotName QUERY_COMMAND = DotName.createSimple(QueryCommand.class.getName());

    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Resource.class, FormCommand.class, QueryCommand.class))
            .addBuildChainCustomizer(new Consumer<>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation.forFields()
                                    .whenField(new java.util.function.Predicate<FieldInfo>() {
                                        @Override
                                        public boolean test(FieldInfo field) {
                                            DotName declaringClass = field.declaringClass().name();
                                            return declaringClass.equals(FORM_COMMAND) || declaringClass.equals(QUERY_COMMAND);
                                        }
                                    })
                                    .transform(new Consumer<AnnotationTransformation.TransformationContext>() {
                                        @Override
                                        public void accept(AnnotationTransformation.TransformationContext ctx) {
                                            FieldInfo field = ctx.declaration().asField();
                                            Class<? extends java.lang.annotation.Annotation> annotation = field
                                                    .declaringClass().name().equals(FORM_COMMAND)
                                                            ? FormParam.class
                                                            : QueryParam.class;
                                            ctx.add(AnnotationInstance.builder(annotation).add("value", field.name()).build());
                                        }
                                    })));
                        }
                    }).produces(AnnotationsTransformerBuildItem.class).build();
                }
            });

    @Test
    public void formParamAddedByTransformer() {
        RestAssured.given()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("cacheName", "users")
                .post("/commands/clear-cache")
                .then()
                .statusCode(200)
                .body(equalTo("cleared users"));
    }

    @Test
    public void queryParamAddedByTransformer() {
        RestAssured.get("/commands/find?name=alice")
                .then()
                .statusCode(200)
                .body(equalTo("found alice"));
    }

    @Path("/commands")
    public static class Resource {

        @POST
        @Path("/clear-cache")
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String clearCache(@BeanParam FormCommand command) {
            return "cleared " + command.cacheName;
        }

        @GET
        @Path("/find")
        public String find(@BeanParam QueryCommand command) {
            return "found " + command.name;
        }
    }

    public static class FormCommand {
        public String cacheName;
    }

    public static class QueryCommand {
        public String name;
    }
}
