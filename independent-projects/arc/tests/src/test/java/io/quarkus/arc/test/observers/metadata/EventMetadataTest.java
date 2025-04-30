package io.quarkus.arc.test.observers.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class EventMetadataTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(BigObserver.class, Emitter.class);

    @Test
    public void testMetadata() {
        Arc.container().beanManager().getEvent().fire(BigDecimal.ONE);
        EventMetadata metadata = BigObserver.METADATA.get();
        assertNotNull(metadata);
        assertEquals(2, metadata.getQualifiers().size());
        for (Annotation qualifier : metadata.getQualifiers()) {
            assertTrue(qualifier.annotationType().equals(Any.class) || qualifier.annotationType().equals(Default.class));
        }
        assertEquals(BigDecimal.class, metadata.getType());
        assertNull(metadata.getInjectionPoint());

        InstanceHandle<Emitter> emitterHandle = Arc.container().instance(Emitter.class);
        emitterHandle.get().doFire(BigInteger.ONE);
        metadata = BigObserver.METADATA.get();
        assertNotNull(metadata);
        assertEquals(2, metadata.getQualifiers().size());
        for (Annotation qualifier : metadata.getQualifiers()) {
            assertTrue(qualifier.annotationType().equals(Any.class) || qualifier.annotationType().equals(Default.class));
        }
        assertEquals(BigInteger.class, metadata.getType());
        InjectionPoint ip = metadata.getInjectionPoint();
        assertNotNull(ip);
        assertEquals(emitterHandle.getBean(), ip.getBean());
        assertEquals("event", ip.getMember().getName());
        assertTrue(ip.isTransient());
        assertNotNull(ip.getAnnotated());
        assertEquals(1, metadata.getInjectionPoint().getQualifiers().size());
        assertEquals(Default.class, metadata.getInjectionPoint().getQualifiers().iterator().next().annotationType());
        assertTrue(ip.getAnnotated() instanceof AnnotatedField);
        assertEquals(ip.getMember(), ((AnnotatedField<?>) ip.getAnnotated()).getJavaMember());
    }

    @Singleton
    static class BigObserver {

        static final AtomicReference<EventMetadata> METADATA = new AtomicReference<EventMetadata>();

        void observe(@Observes BigDecimal value, EventMetadata metadata) {
            METADATA.set(metadata);
        }

        void observe(@Observes BigInteger value, EventMetadata metadata) {
            METADATA.set(metadata);
        }

    }

    @Dependent
    static class Emitter {

        @Inject
        transient Event<BigInteger> event;

        void doFire(BigInteger payload) {
            event.fire(payload);
        }

    }

}
