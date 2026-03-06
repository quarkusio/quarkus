package io.quarkus.kafka.streams.runtime;

import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ManagedContext;

/**
 * A {@link ProcessorSupplier} that creates processor instances via CDI and activates
 * CDI request context for each {@code process()} call.
 * <p>
 * Kafka Streams manages its own threads, which means processors created by the framework
 * have no CDI context. This supplier solves that by:
 * <ul>
 * <li>Creating processor instances from the Arc CDI container (enabling {@code @Inject})</li>
 * <li>Activating a CDI request context around each {@code process()} call</li>
 * </ul>
 * <p>
 * The processor class should be a {@code @Dependent} scoped CDI bean so that each
 * stream task gets its own instance.
 * <p>
 * Usage:
 *
 * <pre>
 * &#64;Dependent
 * public class MyProcessor implements Processor&lt;String, String, String, String&gt; {
 *     &#64;Inject
 *     SomeService service;
 *
 *     &#64;Override
 *     public void process(Record&lt;String, String&gt; record) {
 *         service.handle(record.value());
 *     }
 * }
 *
 * // In your topology builder:
 * builder.stream("input")
 *         .process(CdiAwareProcessorSupplier.of(MyProcessor.class));
 * </pre>
 *
 * @param <KIn> the input key type
 * @param <VIn> the input value type
 * @param <KOut> the output key type
 * @param <VOut> the output value type
 */
public class CdiAwareProcessorSupplier<KIn, VIn, KOut, VOut> implements ProcessorSupplier<KIn, VIn, KOut, VOut> {

    private final Class<? extends Processor<KIn, VIn, KOut, VOut>> processorClass;

    private CdiAwareProcessorSupplier(Class<? extends Processor<KIn, VIn, KOut, VOut>> processorClass) {
        this.processorClass = processorClass;
    }

    /**
     * Creates a new CDI-aware processor supplier for the given processor class.
     *
     * @param processorClass a CDI bean class implementing {@link Processor}
     * @param <KIn> the input key type
     * @param <VIn> the input value type
     * @param <KOut> the output key type
     * @param <VOut> the output value type
     * @return a new processor supplier
     */
    public static <KIn, VIn, KOut, VOut> CdiAwareProcessorSupplier<KIn, VIn, KOut, VOut> of(
            Class<? extends Processor<KIn, VIn, KOut, VOut>> processorClass) {
        return new CdiAwareProcessorSupplier<>(processorClass);
    }

    @Override
    public Processor<KIn, VIn, KOut, VOut> get() {
        InstanceHandle<? extends Processor<KIn, VIn, KOut, VOut>> handle = Arc.container().instance(processorClass);
        if (!handle.isAvailable()) {
            throw new IllegalStateException(
                    "CDI bean not found for processor class: " + processorClass.getName()
                            + ". Ensure it is a CDI bean (e.g. annotated with @Dependent).");
        }
        return new ContextAwareProcessor<>(handle);
    }

    private static class ContextAwareProcessor<KIn, VIn, KOut, VOut> implements Processor<KIn, VIn, KOut, VOut> {

        private final InstanceHandle<? extends Processor<KIn, VIn, KOut, VOut>> handle;
        private final Processor<KIn, VIn, KOut, VOut> delegate;

        ContextAwareProcessor(InstanceHandle<? extends Processor<KIn, VIn, KOut, VOut>> handle) {
            this.handle = handle;
            this.delegate = handle.get();
        }

        @Override
        public void init(ProcessorContext<KOut, VOut> context) {
            delegate.init(context);
        }

        @Override
        public void process(Record<KIn, VIn> record) {
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
