package io.quarkus.resteasy.reactive.server.test.transformation;

import static io.restassured.RestAssured.*;
import static org.jboss.jandex.AnnotationInstance.create;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.function.Consumer;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationsTransformer;
import org.jboss.resteasy.reactive.common.processor.transformation.Transformation;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.resteasy.reactive.server.spi.AnnotationsTransformerBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class AnnotationTransformationTest {

    private static final DotName MY_GET = DotName.createSimple(MyGet.class.getName());
    private static final DotName MY_QUERY = DotName.createSimple(MyQuery.class.getName());

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .addBuildChainCustomizer(new Consumer<>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
                                @Override
                                public boolean appliesTo(AnnotationTarget.Kind kind) {
                                    return kind == AnnotationTarget.Kind.METHOD;
                                }

                                @Override
                                public void transform(TransformationContext transformationContext) {
                                    AnnotationTarget target = transformationContext.getTarget();
                                    if (target.kind() != AnnotationTarget.Kind.METHOD) {
                                        return;
                                    }
                                    MethodInfo methodInfo = target.asMethod();
                                    Transformation transform = transformationContext.transform();
                                    boolean complete = false;

                                    if (methodInfo.hasAnnotation(MY_GET)) {
                                        complete = true;
                                        transform.add(GET);
                                    }

                                    for (AnnotationInstance annotation : methodInfo.annotations()) {
                                        if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                                            if (annotation.name().equals(MY_QUERY)) {
                                                complete = true;
                                                AnnotationValue annotationValue = annotation.value();
                                                transform.add(create(QUERY_PARAM, annotation.target(),
                                                        Collections.singletonList(annotationValue)));
                                            }
                                        }
                                    }

                                    if (complete) {
                                        transform.done();
                                    }
                                }
                            }));
                        }
                    }).produces(AnnotationsTransformerBuildItem.class).build();
                }
            })
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestResource.class, MyGet.class, MyQuery.class));

    @Test
    public void testNoPath() {
        get("/test")
                .then().statusCode(200)
                .and().body(Matchers.equalTo("no path"))
                .and().contentType("text/plain");
    }

    @Test
    public void testHello() {
        get("/test/hello")
                .then().statusCode(200)
                .and().body(Matchers.equalTo("hello world"))
                .and().contentType("text/plain");

        get("/test/hello?nm=foo")
                .then().statusCode(200)
                .and().body(Matchers.equalTo("hello foo"))
                .and().contentType("text/plain");
    }

    @Path("test")
    public static class TestResource {

        @MyGet
        public String noPath() {
            return "no path";
        }

        @Path("hello")
        @MyGet
        public String hello(@MyQuery("nm") @DefaultValue("world") String name) {
            return "hello " + name;
        }

    }

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyGet {

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.PARAMETER })
    public @interface MyQuery {
        String value();
    }
}
