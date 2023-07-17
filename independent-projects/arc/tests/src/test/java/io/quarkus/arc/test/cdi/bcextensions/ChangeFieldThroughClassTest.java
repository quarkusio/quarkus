package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.inject.build.compatible.spi.AnnotationBuilder;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class ChangeFieldThroughClassTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyQualifier.class, MyService.class, MyFooService.class, MyBarService.class, MyServiceConsumer.class)
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() {
        MyServiceConsumer myServiceConsumer = Arc.container().select(MyServiceConsumer.class).get();
        assertTrue(myServiceConsumer.myService instanceof MyBarService);
    }

    public static class MyExtension implements BuildCompatibleExtension {
        @Enhancement(types = MyServiceConsumer.class)
        public void service(ClassConfig clazz) {
            clazz.fields()
                    .stream()
                    .filter(it -> "myService".equals(it.info().name()))
                    .forEach(field -> field.addAnnotation(AnnotationBuilder.of(MyQualifier.class).build()));
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
    static class MyFooService implements MyService {
        @Override
        public String hello() {
            return "foo";
        }
    }

    @Singleton
    @MyQualifier
    static class MyBarService implements MyService {
        @Override
        public String hello() {
            return "bar";
        }
    }

    @Singleton
    static class MyServiceConsumer {
        @Inject
        MyService myService;
    }
}
