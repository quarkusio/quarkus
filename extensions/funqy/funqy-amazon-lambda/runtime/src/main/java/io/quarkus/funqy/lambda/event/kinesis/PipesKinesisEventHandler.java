package io.quarkus.funqy.lambda.event.kinesis;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse.BatchItemFailure;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;

import io.quarkus.funqy.lambda.config.FunqyAmazonConfig;
import io.quarkus.funqy.lambda.event.EventHandler;
import io.quarkus.funqy.lambda.model.kinesis.PipesKinesisEvent;

public class PipesKinesisEventHandler
        implements EventHandler<List<PipesKinesisEvent>, PipesKinesisEvent, StreamsEventResponse> {

    @Override
    public Stream<PipesKinesisEvent> streamEvent(List<PipesKinesisEvent> event, FunqyAmazonConfig amazonConfig) {
        if (event == null) {
            return Stream.empty();
        }
        return event.stream();
    }

    @Override
    public String getIdentifier(PipesKinesisEvent message, FunqyAmazonConfig amazonConfig) {
        return message.getSequenceNumber();
    }

    @Override
    public Supplier<InputStream> getBody(PipesKinesisEvent message, FunqyAmazonConfig amazonConfig) {
        if (message.getData() == null) {
            return ByteArrayInputStream::nullInputStream;
        }
        return () -> new ByteBufferBackedInputStream(message.getData());
    }

    @Override
    public StreamsEventResponse createResponse(List<String> failures, FunqyAmazonConfig amazonConfig) {
        if (!amazonConfig.advancedEventHandling().kinesis().reportBatchItemFailures()) {
            return null;
        }
        return StreamsEventResponse.builder().withBatchItemFailures(
                failures.stream().map(id -> BatchItemFailure.builder().withItemIdentifier(id).build()).toList()).build();
    }

    @Override
    public Class<PipesKinesisEvent> getMessageClass() {
        return PipesKinesisEvent.class;
    }
}
