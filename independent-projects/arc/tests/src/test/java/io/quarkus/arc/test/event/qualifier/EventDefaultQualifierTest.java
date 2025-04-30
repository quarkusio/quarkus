package io.quarkus.arc.test.event.qualifier;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.EventMetadata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class EventDefaultQualifierTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(ObservingBean.class).build();

    @Test
    public void testDefaultQualifierPresent()
            throws ExecutionException, InterruptedException, TimeoutException {
        BeanManager bm = Arc.container().beanManager();
        ObservingBean bean = Arc.container().select(ObservingBean.class).get();
        bean.reset();

        Set<Annotation> expectedQualifiers = Set.of(Any.Literal.INSTANCE, Default.Literal.INSTANCE);

        // just get event fire right away - @Default should be included
        bm.getEvent().fire(new Payload());
        Assertions.assertEquals(1, bean.getDefaultObjectNotified());
        Assertions.assertEquals(1, bean.getDefaultPayloadNotified());
        Assertions.assertEquals(expectedQualifiers, bean.getDefaultObjectQualifiers());
        Assertions.assertEquals(expectedQualifiers, bean.getDefaultPayloadQualifiers());

        // select Payload and fire - @Default should be included
        bm.getEvent().select(Payload.class).fire(new Payload());
        Assertions.assertEquals(2, bean.getDefaultObjectNotified());
        Assertions.assertEquals(2, bean.getDefaultPayloadNotified());
        Assertions.assertEquals(expectedQualifiers, bean.getDefaultObjectQualifiers());
        Assertions.assertEquals(expectedQualifiers, bean.getDefaultPayloadQualifiers());

        // select Payload and explicitly add @Any qualifier, then fire - @Default should *not* be included
        // therefore no notifications should occur
        bm.getEvent().select(Payload.class, Any.Literal.INSTANCE).fire(new Payload());
        Assertions.assertEquals(2, bean.getDefaultObjectNotified());
        Assertions.assertEquals(2, bean.getDefaultPayloadNotified());

        // same in async variant
        // just get event fire right away - @Default should be included
        bm.getEvent().fireAsync(new Payload()).toCompletableFuture().get(2, TimeUnit.SECONDS);
        Assertions.assertEquals(1, bean.getDefaultObjectAsyncNotified());
        Assertions.assertEquals(1, bean.getDefaultPayloadAsyncNotified());
        Assertions.assertEquals(expectedQualifiers, bean.getDefaultObjectAsyncQualifiers());
        Assertions.assertEquals(expectedQualifiers, bean.getDefaultPayloadAsyncQualifiers());

        // select Payload and fire - @Default should be included
        bm.getEvent().select(Payload.class).fireAsync(new Payload()).toCompletableFuture().get(2, TimeUnit.SECONDS);
        Assertions.assertEquals(2, bean.getDefaultObjectAsyncNotified());
        Assertions.assertEquals(2, bean.getDefaultPayloadAsyncNotified());
        Assertions.assertEquals(expectedQualifiers, bean.getDefaultObjectAsyncQualifiers());
        Assertions.assertEquals(expectedQualifiers, bean.getDefaultPayloadAsyncQualifiers());

        // select Payload and explicitly add @Any qualifier, then fire - @Default should *not* be included
        // therefore no notifications should occur
        bm.getEvent().select(Payload.class, Any.Literal.INSTANCE).fireAsync(new Payload()).toCompletableFuture().get(2,
                TimeUnit.SECONDS);
        Assertions.assertEquals(2, bean.getDefaultObjectAsyncNotified());
        Assertions.assertEquals(2, bean.getDefaultPayloadAsyncNotified());
    }

    public static class Payload {
    }

    @ApplicationScoped
    public static class ObservingBean {

        private volatile int defaultObjectNotified = 0;
        private volatile int defaultObjectAsyncNotified = 0;
        private volatile int defaultPayloadNotified = 0;
        private volatile int defaultPayloadAsyncNotified = 0;
        private volatile Set<Annotation> defaultObjectQualifiers;
        private volatile Set<Annotation> defaultObjectAsyncQualifiers;
        private volatile Set<Annotation> defaultPayloadQualifiers;
        private volatile Set<Annotation> defaultPayloadAsyncQualifiers;

        public void observeDefaultObject(@Observes @Default Object payload, EventMetadata em) {
            // object type is very broad, only look for Payload runtime type
            if (em.getType().equals(Payload.class)) {
                this.defaultObjectNotified++;
                this.defaultObjectQualifiers = em.getQualifiers();
            }
        }

        public void observeDefaultPayload(@Observes @Default Payload payload, EventMetadata em) {
            this.defaultPayloadNotified++;
            this.defaultPayloadQualifiers = em.getQualifiers();
        }

        public void observeDefaultObjectAsync(@ObservesAsync @Default Object payload, EventMetadata em) {
            // object type is very broad, only look for Payload runtime type
            if (em.getType().equals(Payload.class)) {
                this.defaultObjectAsyncNotified++;
                this.defaultObjectAsyncQualifiers = em.getQualifiers();
            }
        }

        public void observeDefaultPayloadAsync(@ObservesAsync @Default Payload payload, EventMetadata em) {
            this.defaultPayloadAsyncNotified++;
            this.defaultPayloadAsyncQualifiers = em.getQualifiers();
        }

        public int getDefaultObjectNotified() {
            return defaultObjectNotified;
        }

        public int getDefaultPayloadNotified() {
            return defaultPayloadNotified;
        }

        public Set<Annotation> getDefaultObjectQualifiers() {
            return defaultObjectQualifiers;
        }

        public Set<Annotation> getDefaultPayloadQualifiers() {
            return defaultPayloadQualifiers;
        }

        public int getDefaultObjectAsyncNotified() {
            return defaultObjectAsyncNotified;
        }

        public int getDefaultPayloadAsyncNotified() {
            return defaultPayloadAsyncNotified;
        }

        public Set<Annotation> getDefaultObjectAsyncQualifiers() {
            return defaultObjectAsyncQualifiers;
        }

        public Set<Annotation> getDefaultPayloadAsyncQualifiers() {
            return defaultPayloadAsyncQualifiers;
        }

        public void reset() {
            this.defaultPayloadNotified = 0;
            this.defaultPayloadAsyncNotified = 0;
            this.defaultObjectNotified = 0;
            this.defaultObjectAsyncNotified = 0;
            this.defaultObjectQualifiers = null;
            this.defaultObjectAsyncQualifiers = null;
            this.defaultPayloadQualifiers = null;
            this.defaultPayloadAsyncQualifiers = null;
        }
    }
}
