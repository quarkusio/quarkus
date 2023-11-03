package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.MetaAnnotations;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class CustomQualifierTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() {
        MyService myService = Arc.container().select(MyService.class, new MyAnnotationLiteral("something")).get();
        assertEquals("bar", myService.hello());
    }

    public static class MyExtension implements BuildCompatibleExtension {
        @Discovery
        public void discovery(MetaAnnotations meta, ScannedClasses scan) {
            scan.add(MyServiceFoo.class.getName());
            scan.add(MyServiceBar.class.getName());

            ClassConfig cfg = meta.addQualifier(MyAnnotation.class);
            cfg.methods()
                    .stream()
                    .filter(it -> "value".equals(it.info().name()))
                    .forEach(it -> it.addAnnotation(Nonbinding.class));
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

    interface MyService {
        String hello();
    }

    @Singleton
    static class MyServiceFoo implements MyService {
        @Override
        public String hello() {
            return "foo";
        }
    }

    @Singleton
    @MyAnnotation("this should be ignored, the value member should be treated as @Nonbinding")
    static class MyServiceBar implements MyService {
        @Override
        public String hello() {
            return "bar";
        }
    }
}
