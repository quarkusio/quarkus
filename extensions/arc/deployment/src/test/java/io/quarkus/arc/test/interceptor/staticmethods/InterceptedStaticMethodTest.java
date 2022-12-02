package io.quarkus.arc.test.interceptor.staticmethods;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opentest4j.AssertionFailedError;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

public class InterceptedStaticMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(InterceptMe.class, Simple.class, AnotherSimple.class, SimpleInterceptor.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {

                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

                            @Override
                            public boolean appliesTo(Kind kind) {
                                return AnnotationTarget.Kind.METHOD == kind;
                            }

                            @Override
                            public void transform(TransformationContext context) {
                                MethodInfo method = context.getTarget().asMethod();
                                if (method.declaringClass().name().toString()
                                        .endsWith("AnotherSimple")) {
                                    context.transform().add(InterceptMe.class).done();
                                }
                            }

                        }));
                    }
                }).produces(AnnotationsTransformerBuildItem.class).build();
            }
        };
    }

    @Test
    public void testInterceptor() {
        assertEquals("OK:PONG", Simple.ping("pong"));
        Simple.pong();
        assertEquals(42.0, Simple.testDouble(2.0, "foo", 5, null));
        assertEquals(1, SimpleInterceptor.VOID_INTERCEPTIONS.get());
        assertEquals("OK:PONG", AnotherSimple.ping("pong"));
    }

    public static class Simple {

        @InterceptMe
        public static String ping(String val) {
            return val.toUpperCase();
        }

        @InterceptMe
        static void pong() {
        }

        @InterceptMe
        protected static Double testDouble(double val, String str, int num, Simple parent) {
            return val;
        }

    }

    public static class AnotherSimple {

        // @InterceptMe is added by the transformer
        public static String ping(String val) {
            return val.toUpperCase();
        }
    }

    @Priority(1)
    @Interceptor
    @InterceptMe
    static class SimpleInterceptor {

        static final AtomicInteger VOID_INTERCEPTIONS = new AtomicInteger();

        @AroundInvoke
        Object aroundInvoke(InvocationContext ctx) throws Exception {
            if (!Modifier.isStatic(ctx.getMethod().getModifiers())) {
                throw new AssertionFailedError("Not a static method!");
            }
            assertNull(ctx.getTarget());
            Object ret = ctx.proceed();
            if (ret != null) {
                if (ret instanceof String) {
                    return "OK:" + ctx.proceed();
                } else if (ret instanceof Double) {
                    return 42.0;
                } else {
                    throw new AssertionFailedError("Unsupported return type: " + ret.getClass());
                }
            } else {
                VOID_INTERCEPTIONS.incrementAndGet();
                return ret;
            }
        }

    }

    @InterceptorBinding
    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @interface InterceptMe {

    }

}
