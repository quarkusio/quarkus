package io.quarkus.arc.test.injectionpoint;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.function.Consumer;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;

@Dependent
public class BeanWithInjectionPointMetadata {
    @Inject
    InjectionPoint field;

    InjectionPoint constructor;

    InjectionPoint initializer;

    @Inject
    public BeanWithInjectionPointMetadata(InjectionPoint ip) {
        this.constructor = ip;
    }

    @Inject
    void init(InjectionPoint ip) {
        this.initializer = ip;
    }

    public void assertPresent(Consumer<InjectionPoint> asserter) {
        assertNotNull(field);
        assertNotNull(constructor);
        assertNotNull(initializer);

        if (asserter != null) {
            asserter.accept(field);
            asserter.accept(constructor);
            asserter.accept(initializer);
        }
    }

    public void assertAbsent() {
        assertNull(field);
        assertNull(constructor);
        assertNull(initializer);
    }
}
