package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.MetaAnnotations;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class CustomStereotypeTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() {
        InstanceHandle<MyService> bean = Arc.container().select(MyService.class).getHandle();
        assertEquals(ApplicationScoped.class, bean.getBean().getScope());
        assertEquals("Hello!", bean.get().hello());
    }

    public static class MyExtension implements BuildCompatibleExtension {
        @Discovery
        public void discovery(MetaAnnotations meta, ScannedClasses scan) {
            scan.add(MyService.class.getName());

            ClassConfig cfg = meta.addStereotype(MyAnnotation.class);
            cfg.addAnnotation(ApplicationScoped.class);
        }
    }

    // ---

    @Retention(RetentionPolicy.RUNTIME)
    @interface MyAnnotation {
    }

    @MyAnnotation
    static class MyService {
        String hello() {
            return "Hello!";
        }
    }
}
