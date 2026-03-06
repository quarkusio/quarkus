package io.quarkus.it.kafka.streams;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

import io.quarkus.arc.Unremovable;

@Dependent
@Unremovable
public class TrackingProcessor implements Processor<Integer, EnrichedCustomer, Void, Void> {

    @Inject
    RequestScopedHandler handler;

    @Override
    public void init(ProcessorContext<Void, Void> context) {
    }

    @Override
    public void process(Record<Integer, EnrichedCustomer> record) {
        // This call goes through a @RequestScoped proxy.
        // If CdiAwareProcessorSupplier did NOT activate request context,
        // this would throw ContextNotActiveException.
        handler.handle(record.value().name);
    }

    @Override
    public void close() {
    }
}
