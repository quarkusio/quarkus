package io.quarkus.it.kafka.streams;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;

import io.quarkus.arc.Unremovable;

@Dependent
@Unremovable
public class TrackingFixedKeyProcessor implements FixedKeyProcessor<Integer, EnrichedCustomer, EnrichedCustomer> {

    @Inject
    FixedKeyProcessorTracker tracker;

    private FixedKeyProcessorContext<Integer, EnrichedCustomer> context;

    @Override
    public void init(FixedKeyProcessorContext<Integer, EnrichedCustomer> context) {
        this.context = context;
    }

    @Override
    public void process(FixedKeyRecord<Integer, EnrichedCustomer> record) {
        tracker.track(record.value().name);
        context.forward(record);
    }

    @Override
    public void close() {
    }
}
