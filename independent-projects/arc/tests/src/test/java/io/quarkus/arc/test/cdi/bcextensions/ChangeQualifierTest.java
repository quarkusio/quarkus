package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.FieldConfig;
import jakarta.enterprise.inject.build.compatible.spi.Messages;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class ChangeQualifierTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyQualifier.class, MyService.class, MyServiceConsumer.class)
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() {
        MyServiceConsumer myServiceConsumer = Arc.container().select(MyServiceConsumer.class).get();
        assertTrue(myServiceConsumer.myService instanceof MyBarService);
    }

    public static class MyExtension implements BuildCompatibleExtension {
        @Discovery
        public void services(ScannedClasses classes, Messages messages) {
            classes.add(MyFooService.class.getName());
            classes.add(MyBarService.class.getName());
            classes.add(MyBazService.class.getName());
        }

        @Enhancement(types = MyFooService.class)
        public void foo(ClassConfig clazz, Messages messages) {
            clazz.removeAnnotation(ann -> ann.name().equals(MyQualifier.class.getName()));
        }

        @Enhancement(types = MyBarService.class)
        public void bar(ClassConfig clazz, Messages messages) {
            clazz.addAnnotation(MyQualifier.class);
        }

        @Enhancement(types = MyServiceConsumer.class)
        public void service(FieldConfig field, Messages messages) {
            if ("myService".equals(field.info().name())) {
                field.addAnnotation(MyQualifier.class);
            }
        }
    }

    // ---

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyQualifier {
    }

    interface MyService {
        String hello();
    }

    @Singleton
    @MyQualifier
    static class MyFooService implements MyService {
        private final String value = "foo";

        @Override
        public String hello() {
            return value;
        }
    }

    @Singleton
    static class MyBarService implements MyService {
        private static final String VALUE = "bar";

        @Override
        public String hello() {
            return VALUE;
        }
    }

    @Singleton
    static class MyBazService implements MyService {
        @Override
        public String hello() {
            throw new UnsupportedOperationException();
        }
    }

    @Singleton
    static class MyServiceConsumer {
        @Inject
        MyService myService;
    }
}
