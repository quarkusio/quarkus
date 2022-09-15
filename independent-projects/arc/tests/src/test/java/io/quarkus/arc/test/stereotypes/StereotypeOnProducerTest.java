package io.quarkus.arc.test.stereotypes;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Named;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class StereotypeOnProducerTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(StereotypeOnProducerTest.class,
                    BeNamed.class, A.class, B.class)
            .build();

    @Test
    public void testProducerField() {
        assertNotNull(Arc.container().instance("zzz").get());
    }

    @Test
    public void testProducerMethod() {
        assertNotNull(Arc.container().instance("yyy").get());
    }

    @Named
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BeNamed {
    }

    static class A {

    }

    static class B {

    }

    @Produces
    @RequestScoped
    @BeNamed
    A zzz = new A();

    @Produces
    @RequestScoped
    @BeNamed
    B yyy() {
        return new B();
    }
}
