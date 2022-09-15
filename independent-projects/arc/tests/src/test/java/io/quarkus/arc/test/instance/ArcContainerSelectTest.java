package io.quarkus.arc.test.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ArcContainerSelectTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Alpha.class, Washcloth.class);

    @SuppressWarnings("serial")
    @Test
    public void testSelect() {
        assertTrue(Arc.container().select(BeanManager.class).isResolvable());
        InjectableInstance<Supplier<String>> instance = Arc.container().select(new TypeLiteral<Supplier<String>>() {
        });
        Set<String> strings = new HashSet<>();
        for (InstanceHandle<Supplier<String>> handle : instance.handles()) {
            strings.add(handle.get().get());
            handle.close();
        }
        assertEquals(2, strings.size());
        assertTrue(strings.contains("alpha"));
        assertTrue(strings.contains("washcloth"));
        assertTrue(Washcloth.INIT.get());
        assertTrue(Washcloth.DESTROYED.get());
    }

    @Singleton
    static class Alpha implements Supplier<String> {

        @Override
        public String get() {
            return "alpha";
        }

    }

    @Dependent
    static class Washcloth implements Supplier<String> {

        static final AtomicBoolean INIT = new AtomicBoolean(false);
        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

        @Inject
        InjectionPoint injectionPoint;

        @PostConstruct
        void init() {
            assertNotNull(injectionPoint);
            assertEquals(0, injectionPoint.getQualifiers().size());
            Type requiredType = injectionPoint.getType();
            assertTrue(requiredType instanceof ParameterizedType);
            assertEquals(Supplier.class, ((ParameterizedType) requiredType).getRawType());
            INIT.set(true);
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

        @Override
        public String get() {
            return "washcloth";
        }

    }

}
