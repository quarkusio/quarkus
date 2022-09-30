package io.quarkus.arc.test.interceptor.staticmethods;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opentest4j.AssertionFailedError;

import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.MethodMetadata;
import io.quarkus.arc.Reflectionless;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

public class InterceptedStaticMethodReflectionlessTest {
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
                                return Kind.METHOD == kind;
                            }

                            @Override
                            public void transform(TransformationContext context) {
                                MethodInfo method = context.getTarget().asMethod();
                                if (method.declaringClass().name().toString().endsWith("AnotherSimple")) {
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
        assertNotNull(SimpleInterceptor.method);
        assertFalse(SimpleInterceptor.method.isConstructor());
        assertTrue(SimpleInterceptor.method.isStatic());
        assertEquals("ping", SimpleInterceptor.method.getName());
        assertEquals(Simple.class, SimpleInterceptor.method.getDeclaringClass());
        assertEquals(1, SimpleInterceptor.method.getParameterTypes().length);
        assertEquals(String.class, SimpleInterceptor.method.getParameterTypes()[0]);
        assertTrue(SimpleInterceptor.method.isAnnotationPresent(InterceptMe.class));
        assertTrue(SimpleInterceptor.method.isAnnotationPresent(MySimpleAnnotation.class));
        assertNotNull(SimpleInterceptor.method.getAnnotation(InterceptMe.class));
        assertNotNull(SimpleInterceptor.method.getAnnotation(MySimpleAnnotation.class));
        assertEquals("foo", SimpleInterceptor.method.getAnnotation(MySimpleAnnotation.class).value());
        assertEquals(2, SimpleInterceptor.method.getAnnotations().length);

        Simple.pong();
        assertNotNull(SimpleInterceptor.method);
        assertFalse(SimpleInterceptor.method.isConstructor());
        assertTrue(SimpleInterceptor.method.isStatic());
        assertEquals("pong", SimpleInterceptor.method.getName());
        assertEquals(Simple.class, SimpleInterceptor.method.getDeclaringClass());
        assertEquals(0, SimpleInterceptor.method.getParameterTypes().length);
        assertTrue(SimpleInterceptor.method.isAnnotationPresent(InterceptMe.class));
        assertFalse(SimpleInterceptor.method.isAnnotationPresent(MySimpleAnnotation.class));
        assertNotNull(SimpleInterceptor.method.getAnnotation(InterceptMe.class));
        assertNull(SimpleInterceptor.method.getAnnotation(MySimpleAnnotation.class));
        assertEquals(1, SimpleInterceptor.method.getAnnotations().length);

        assertEquals(42.0, Simple.testDouble(2.0, "foo", 5, null));
        assertEquals(1, SimpleInterceptor.VOID_INTERCEPTIONS.get());
        assertNotNull(SimpleInterceptor.method);
        assertFalse(SimpleInterceptor.method.isConstructor());
        assertTrue(SimpleInterceptor.method.isStatic());
        assertEquals("testDouble", SimpleInterceptor.method.getName());
        assertEquals(Simple.class, SimpleInterceptor.method.getDeclaringClass());
        assertEquals(4, SimpleInterceptor.method.getParameterTypes().length);
        assertEquals(double.class, SimpleInterceptor.method.getParameterTypes()[0]);
        assertEquals(String.class, SimpleInterceptor.method.getParameterTypes()[1]);
        assertEquals(int.class, SimpleInterceptor.method.getParameterTypes()[2]);
        assertEquals(Simple.class, SimpleInterceptor.method.getParameterTypes()[3]);
        assertTrue(SimpleInterceptor.method.isAnnotationPresent(InterceptMe.class));
        assertTrue(SimpleInterceptor.method.isAnnotationPresent(MySimpleAnnotation.class));
        assertNotNull(SimpleInterceptor.method.getAnnotation(InterceptMe.class));
        assertNotNull(SimpleInterceptor.method.getAnnotation(MySimpleAnnotation.class));
        assertEquals("bar", SimpleInterceptor.method.getAnnotation(MySimpleAnnotation.class).value());
        assertEquals(2, SimpleInterceptor.method.getAnnotations().length);

        assertEquals("OK:PONG", AnotherSimple.ping("pong"));
        assertNotNull(SimpleInterceptor.method);
        assertFalse(SimpleInterceptor.method.isConstructor());
        assertTrue(SimpleInterceptor.method.isStatic());
        assertEquals("ping", SimpleInterceptor.method.getName());
        assertEquals(AnotherSimple.class, SimpleInterceptor.method.getDeclaringClass());
        assertEquals(1, SimpleInterceptor.method.getParameterTypes().length);
        assertEquals(String.class, SimpleInterceptor.method.getParameterTypes()[0]);
        assertTrue(SimpleInterceptor.method.isAnnotationPresent(InterceptMe.class));
        assertTrue(SimpleInterceptor.method.isAnnotationPresent(MySimpleAnnotation.class));
        assertNotNull(SimpleInterceptor.method.getAnnotation(InterceptMe.class));
        assertNotNull(SimpleInterceptor.method.getAnnotation(MySimpleAnnotation.class));
        assertEquals("baz", SimpleInterceptor.method.getAnnotation(MySimpleAnnotation.class).value());
        assertEquals(2, SimpleInterceptor.method.getAnnotations().length);
    }

    public static class Simple {
        @InterceptMe
        @MySimpleAnnotation("foo")
        public static String ping(String val) {
            return val.toUpperCase();
        }

        @InterceptMe
        static void pong() {
        }

        @InterceptMe
        @MySimpleAnnotation("bar")
        protected static Double testDouble(double val, String str, int num, Simple parent) {
            return val;
        }
    }

    public static class AnotherSimple {
        // @InterceptMe is added by the transformer
        @MySimpleAnnotation("baz")
        public static String ping(String val) {
            return val.toUpperCase();
        }
    }

    @Priority(1)
    @Interceptor
    @Reflectionless
    @InterceptMe
    static class SimpleInterceptor {
        static final AtomicInteger VOID_INTERCEPTIONS = new AtomicInteger();

        static MethodMetadata method;

        @AroundInvoke
        Object aroundInvoke(ArcInvocationContext ctx) throws Exception {
            if (!ctx.getMethodMetadata().isStatic()) {
                throw new AssertionFailedError("Not a static method!");
            }
            assertNull(ctx.getTarget());
            assertNull(ctx.getMethod());
            assertNotNull(ctx.getMethodMetadata());
            method = ctx.getMethodMetadata();

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

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @interface MySimpleAnnotation {
        String value();
    }
}
