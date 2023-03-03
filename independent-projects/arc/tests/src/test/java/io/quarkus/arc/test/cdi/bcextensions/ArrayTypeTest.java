package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.types.Type;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class ArrayTypeTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyAnn1.class, MyAnn2.class, MyAnn3.class, MyAnn4.class, MyService.class)
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() throws IOException {
        MyService myService = Arc.container().select(MyService.class).get();
        assertNotNull(myService);

        assertNotNull(MyExtension.type);

        // @MyAnn4 String [] @MyAnn1 [][] @MyAnn2 [][] @MyAnn3 []
        Type type = MyExtension.type;
        assertTrue(type.isArray());
        assertTrue(type.annotations().isEmpty());

        // @MyAnn4 String @MyAnn1 [][] @MyAnn2 [][] @MyAnn3 []
        type = type.asArray().componentType();
        assertTrue(type.isArray());
        assertEquals(1, type.annotations().size());
        assertTrue(type.hasAnnotation(MyAnn1.class));

        // @MyAnn4 String [] @MyAnn2 [][] @MyAnn3 []
        type = type.asArray().componentType();
        assertTrue(type.isArray());
        assertTrue(type.annotations().isEmpty());

        // @MyAnn4 String @MyAnn2 [][] @MyAnn3 []
        type = type.asArray().componentType();
        assertTrue(type.isArray());
        assertEquals(1, type.annotations().size());
        assertTrue(type.hasAnnotation(MyAnn2.class));

        // @MyAnn4 String [] @MyAnn3 []
        type = type.asArray().componentType();
        assertTrue(type.isArray());
        assertTrue(type.annotations().isEmpty());

        // @MyAnn4 String @MyAnn3 []
        type = type.asArray().componentType();
        assertTrue(type.isArray());
        assertEquals(1, type.annotations().size());
        assertTrue(type.hasAnnotation(MyAnn3.class));

        // @MyAnn4 String
        type = type.asArray().componentType();
        assertFalse(type.isArray());
        assertEquals(1, type.annotations().size());
        assertTrue(type.hasAnnotation(MyAnn4.class));
    }

    public static class MyExtension implements BuildCompatibleExtension {
        static Type type;

        @Enhancement(types = MyService.class)
        public void service(ClassInfo clazz) {
            clazz.fields()
                    .stream()
                    .filter(it -> "field".equals(it.name()))
                    .forEach(field -> {
                        MyExtension.type = field.type();
                    });
        }
    }

    // ---

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    @interface MyAnn1 {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    @interface MyAnn2 {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    @interface MyAnn3 {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    @interface MyAnn4 {
    }

    @Singleton
    static class MyService {
        @MyAnn4
        String[] @MyAnn1 [][] @MyAnn2 [][] @MyAnn3 [] field;
    }
}
