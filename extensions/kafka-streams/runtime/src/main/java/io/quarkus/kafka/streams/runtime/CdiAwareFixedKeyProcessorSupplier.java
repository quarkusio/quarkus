package io.quarkus.kafka.streams.runtime;

import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorSupplier;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ManagedContext;

/**
 * A {@link FixedKeyProcessorSupplier} that creates processor instances via CDI and activates
 * CDI request context for each {@code process()} call.
 * <p>
 * See {@link CdiAwareProcessorSupplier} for full documentation. This is the equivalent
 * for {@link FixedKeyProcessor} implementations.
 * <p>
 * Usage:
 *
 * <pre>
 * &#64;Dependent
 * &#64;Unremovable
 * public class MyProcessor implements FixedKeyProcessor&lt;String, String, String&gt; {
 *     &#64;Inject
 *     SomeService service;
 *
 *     &#64;Override
 *     public void process(FixedKeyRecord&lt;String, String&gt; record) {
 *         service.handle(record.value());
 *     }
 * }
 *
 * // In your topology builder:
 * builder.stream("input")
 *         .processValues(CdiAwareFixedKeyProcessorSupplier.of(MyProcessor.class));
 * </pre>
 *
 * @param <KIn> the input key type
 * @param <VIn> the input value type
 * @param <VOut> the output value type
 */
public class CdiAwareFixedKeyProcessorSupplier<KIn, VIn, VOut> implements FixedKeyProcessorSupplier<KIn, VIn, VOut> {

    private final Class<? extends FixedKeyProcessor<KIn, VIn, VOut>> processorClass;

    private CdiAwareFixedKeyProcessorSupplier(Class<? extends FixedKeyProcessor<KIn, VIn, VOut>> processorClass) {
        this.processorClass = processorClass;
    }

    /**
     * Creates a new CDI-aware fixed-key processor supplier for the given processor class.
     *
     * @param processorClass a CDI bean class implementing {@link FixedKeyProcessor}
     * @param <KIn> the input key type
     * @param <VIn> the input value type
     * @param <VOut> the output value type
     * @return a new processor supplier
     */
    public static <KIn, VIn, VOut> CdiAwareFixedKeyProcessorSupplier<KIn, VIn, VOut> of(
            Class<? extends FixedKeyProcessor<KIn, VIn, VOut>> processorClass) {
        return new CdiAwareFixedKeyProcessorSupplier<>(processorClass);
    }

    @Override
    public FixedKeyProcessor<KIn, VIn, VOut> get() {
        InstanceHandle<? extends FixedKeyProcessor<KIn, VIn, VOut>> handle = Arc.container().instance(processorClass);
        if (!handle.isAvailable()) {
            throw new IllegalStateException(
                    "CDI bean not found for processor class: " + processorClass.getName()
                            + ". Ensure it is a CDI bean (e.g. annotated with @Dependent).");
        }
        return new ContextAwareFixedKeyProcessor<>(handle);
    }

    private static class ContextAwareFixedKeyProcessor<KIn, VIn, VOut> implements FixedKeyProcessor<KIn, VIn, VOut> {

        private final InstanceHandle<? extends FixedKeyProcessor<KIn, VIn, VOut>> handle;
        private final FixedKeyProcessor<KIn, VIn, VOut> delegate;

        ContextAwareFixedKeyProcessor(InstanceHandle<? extends FixedKeyProcessor<KIn, VIn, VOut>> handle) {
            this.handle = handle;
            this.delegate = handle.get();
        }

        @Override
        public void init(FixedKeyProcessorContext<KIn, VOut> context) {
            delegate.init(context);
        }

        @Override
        public void process(FixedKeyRecord<KIn, VIn> record) {
            ManagedContext requestContext = Arc.container().requestContext();
            if (requestContext.isActive()) {
                delegate.process(record);
                return;
            }
            try {
                requestContext.activate();
                delegate.process(record);
            } finally {
                requestContext.terminate();
            }
        }

        @Override
        public void close() {
            try {
                delegate.close();
            } finally {
                handle.close();
            }
        }
    }
}
