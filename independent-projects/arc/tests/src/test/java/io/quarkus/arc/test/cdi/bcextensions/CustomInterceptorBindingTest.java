package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.build.compatible.spi.AnnotationBuilder;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.MetaAnnotations;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class CustomInterceptorBindingTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() {
        MyService myService = Arc.container().select(MyService.class).get();
        assertEquals("Intercepted: Hello!", myService.hello());
    }

    public static class MyExtension implements BuildCompatibleExtension {
        @Discovery
        public void discovery(MetaAnnotations meta, ScannedClasses scan) {
            scan.add(MyInterceptor.class.getName());
            scan.add(MyService.class.getName());

            ClassConfig cfg = meta.addInterceptorBinding(MyAnnotation.class);
            cfg.methods()
                    .stream()
                    .filter(it -> "value".equals(it.info().name()))
                    .forEach(it -> it.addAnnotation(Nonbinding.class));
        }

        @Enhancement(types = MyInterceptor.class)
        public void interceptorPriority(ClassConfig clazz) {
            clazz.addAnnotation(AnnotationBuilder.of(Priority.class).value(1).build());
        }
    }

    // ---

    @Retention(RetentionPolicy.RUNTIME)
    @interface MyAnnotation {
        String value();
    }

    static class MyAnnotationLiteral extends AnnotationLiteral<MyAnnotation> implements MyAnnotation {
        private final String value;

        MyAnnotationLiteral(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    @Interceptor
    @MyAnnotation("something")
    static class MyInterceptor {
        @AroundInvoke
        public Object intercept(InvocationContext ctx) throws Exception {
            return "Intercepted: " + ctx.proceed();
        }
    }

    @Singleton
    @MyAnnotation("this should be ignored, the value member should be treated as @Nonbinding")
    static class MyService {
        public String hello() {
            return "Hello!";
        }
    }
}
