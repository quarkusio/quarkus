package io.quarkus.arc.test.decorators.selfinvocation.acme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

// in this test, the iteration order of decorated types in `BeanInfo.findMatchingDecorators()` is:
//
// - io.quarkus.arc.test.decorators.selfinvocation.acme.SelfInvocationDecoratorTest$PayloadHandler<T>
// - io.quarkus.arc.test.decorators.selfinvocation.acme.SelfInvocationDecoratorTest$EnvelopeHandler<T>
//
// there's a copy of this test in the `.example` package where the iteration order is opposite
public class SelfInvocationDecoratorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(GreetingHandler.class, LoggingDecorator.class,
            TestEnvelope.class);

    @Test
    public void test() {
        GreetingHandler.INVOCATIONS.set(0);
        LoggingDecorator.ENVELOPE_CALLS.set(0);

        EnvelopeHandler<String> handler = Arc.container().instance(new TypeLiteral<EnvelopeHandler<String>>() {
        }).get();

        assertThatCode(() -> handler.process("hello", Instant.now())).doesNotThrowAnyException();

        assertThat(LoggingDecorator.ENVELOPE_CALLS.get()).isEqualTo(0);
        assertThat(GreetingHandler.INVOCATIONS.get()).isEqualTo(1);

        handler.process(TestEnvelope.of("hello"));

        assertThat(LoggingDecorator.ENVELOPE_CALLS.get()).isEqualTo(1);
        assertThat(GreetingHandler.INVOCATIONS.get()).isEqualTo(2);
    }

    interface PayloadHandler<T> {
        void process(T payload, Instant timestamp);

        Class<T> payloadType();
    }

    interface EnvelopeHandler<T> extends PayloadHandler<T> {
        @Override
        default void process(T payload, Instant timestamp) {
            throw new UnsupportedOperationException("Use process(Envelope) instead.");
        }

        void process(Envelope<T> envelope);
    }

    interface Envelope<T> {
        T payload();

        Instant timestamp();

        String id();
    }

    static final class TestEnvelope implements Envelope<String> {
        private final String payload;
        private final Instant timestamp;
        private final String id;

        private TestEnvelope(String payload, Instant timestamp, String id) {
            this.payload = payload;
            this.timestamp = timestamp;
            this.id = id;
        }

        static TestEnvelope of(String payload) {
            return new TestEnvelope(payload, Instant.EPOCH, "test");
        }

        @Override
        public String payload() {
            return payload;
        }

        @Override
        public Instant timestamp() {
            return timestamp;
        }

        @Override
        public String id() {
            return id;
        }
    }

    abstract static class AbstractEnvelopeHandler<T> implements EnvelopeHandler<T> {
        public void process(T payload, Instant timestamp, String correlationId) {
            process(payload, timestamp);
        }

        @Override
        public void process(Envelope<T> envelope) {
            process(envelope.payload(), envelope.timestamp(), envelope.id());
        }
    }

    @ApplicationScoped
    static class GreetingHandler extends AbstractEnvelopeHandler<String> {
        static final AtomicInteger INVOCATIONS = new AtomicInteger();

        @Override
        public Class<String> payloadType() {
            return String.class;
        }

        @Override
        public void process(String payload, Instant timestamp) {
            INVOCATIONS.incrementAndGet();
        }
    }

    @Decorator
    @Priority(1)
    @Dependent
    static class LoggingDecorator<T> implements EnvelopeHandler<T> {
        static final AtomicInteger ENVELOPE_CALLS = new AtomicInteger();

        private final EnvelopeHandler<T> delegate;

        @Inject
        LoggingDecorator(@Delegate @Any EnvelopeHandler<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Class<T> payloadType() {
            return delegate.payloadType();
        }

        @Override
        public void process(Envelope<T> envelope) {
            ENVELOPE_CALLS.incrementAndGet();
            delegate.process(envelope);
        }

        @Override
        public void process(T payload, Instant timestamp) {
            delegate.process(payload, timestamp);
        }
    }
}
