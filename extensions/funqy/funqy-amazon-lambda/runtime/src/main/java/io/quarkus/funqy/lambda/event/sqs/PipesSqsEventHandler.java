package io.quarkus.funqy.lambda.event.sqs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import io.quarkus.funqy.lambda.config.FunqyAmazonConfig;
import io.quarkus.funqy.lambda.event.EventHandler;
import io.quarkus.funqy.lambda.model.pipes.BatchItemFailures;
import io.quarkus.funqy.lambda.model.pipes.Response;

public class PipesSqsEventHandler implements EventHandler<List<SQSMessage>, SQSMessage, Response> {

    @Override
    public Stream<SQSMessage> streamEvent(final List<SQSMessage> event, final FunqyAmazonConfig amazonConfig) {
        return event.stream();
    }

    @Override
    public String getIdentifier(SQSMessage message, FunqyAmazonConfig amazonConfig) {
        return message.getMessageId();
    }

    @Override
    public Supplier<InputStream> getBody(SQSMessage message, FunqyAmazonConfig amazonConfig) {
        if (message.getBody() == null) {
            return ByteArrayInputStream::nullInputStream;
        }
        return () -> new ByteArrayInputStream(message.getBody().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Response createResponse(List<String> failures, FunqyAmazonConfig amazonConfig) {
        if (!amazonConfig.advancedEventHandling().sqs().reportBatchItemFailures()) {
            return null;
        }
        return new Response(failures.stream().map(BatchItemFailures::new).toList());
    }

    @Override
    public Class<SQSMessage> getMessageClass() {
        return SQSMessage.class;
    }
}
