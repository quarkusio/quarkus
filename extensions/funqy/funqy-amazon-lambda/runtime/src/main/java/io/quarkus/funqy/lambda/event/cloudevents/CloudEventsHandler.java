package io.quarkus.funqy.lambda.event.cloudevents;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.cloudevents.CloudEvent;
import io.quarkus.funqy.lambda.config.FunqyAmazonConfig;
import io.quarkus.funqy.lambda.event.EventHandler;
import io.quarkus.funqy.lambda.model.pipes.BatchItemFailures;
import io.quarkus.funqy.lambda.model.pipes.Response;

public class CloudEventsHandler implements EventHandler<List<CloudEvent>, CloudEvent, Response> {
    @Override
    public Stream<CloudEvent> streamEvent(final List<CloudEvent> event, final FunqyAmazonConfig amazonConfig) {
        if (event == null) {
            return Stream.empty();
        }
        return event.stream();
    }

    @Override
    public String getIdentifier(final CloudEvent message, final FunqyAmazonConfig amazonConfig) {
        return message.getId();
    }

    @Override
    public Supplier<InputStream> getBody(final CloudEvent message, final FunqyAmazonConfig amazonConfig) {
        return () -> new ByteArrayInputStream(message.getData().toBytes());
    }

    @Override
    public Response createResponse(final List<String> failures, final FunqyAmazonConfig amazonConfig) {
        if (!amazonConfig.advancedEventHandling().sqs().reportBatchItemFailures()) {
            return null;
        }
        return new Response(failures.stream().map(BatchItemFailures::new).toList());
    }

    @Override
    public Class<CloudEvent> getMessageClass() {
        return CloudEvent.class;
    }
}
