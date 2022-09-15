package io.quarkus.arc.test.observers.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.inject.Singleton;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class EventMetadataTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(BigDecimalObserver.class);

    @Test
    public void testMetadata() {
        Arc.container().beanManager().getEvent().fire(BigDecimal.ONE);
        EventMetadata metadata = BigDecimalObserver.METADATA.get();
        assertNotNull(metadata);
        assertEquals(1, metadata.getQualifiers().size());
        assertEquals(Any.class, metadata.getQualifiers().iterator().next().annotationType());
        assertEquals(BigDecimal.class, metadata.getType());
    }

    @Singleton
    static class BigDecimalObserver {

        static final AtomicReference<EventMetadata> METADATA = new AtomicReference<EventMetadata>();

        void observe(@Observes BigDecimal value, EventMetadata metadata) {
            METADATA.set(metadata);
        }

    }

}
