package io.quarkus.arc.test.interceptor.staticmethods;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.jboss.jandex.AnnotationTransformation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opentest4j.AssertionFailedError;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

public class InterceptedStaticMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(InterceptMe.class, NotNull.class, WithClassPolicy.class, Simple.class, AnotherSimple.class,
                            SimpleInterceptor.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {

                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new AnnotationsTransformerBuildItem(
                                AnnotationTransformation.forMethods()
                                        .whenMethod(AnotherSimple.class, "ping")
                                        .transform(tc -> tc.add(InterceptMe.class))));
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

        @WithClassPolicy
        @InterceptMe
        public static String ping(@NotNull String val) {
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
            // verify annotations can be inspected
            if (ctx.getMethod().getDeclaringClass().getName().equals(Simple.class.getName())) {
                assertEquals(1, ctx.getMethod().getAnnotations().length);
                assertTrue(ctx.getMethod().isAnnotationPresent(InterceptMe.class));
                assertFalse(ctx.getMethod().isAnnotationPresent(WithClassPolicy.class));
                assertFalse(ctx.getMethod().isAnnotationPresent(NotNull.class));
                if (ctx.getMethod().getName().equals("ping")) {
                    assertTrue(ctx.getMethod().getParameters()[0].isAnnotationPresent(NotNull.class));
                }
            }
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

    @Retention(RUNTIME)
    @interface NotNull {

    }

    @Retention(CLASS)
    @interface WithClassPolicy {

    }

}
