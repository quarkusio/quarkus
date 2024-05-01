package io.quarkus.funqy.lambda.event.sqs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse.BatchItemFailure;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import io.quarkus.funqy.lambda.config.FunqyAmazonConfig;
import io.quarkus.funqy.lambda.event.EventHandler;

public class SqsEventHandler implements EventHandler<SQSEvent, SQSMessage, SQSBatchResponse> {

    @Override
    public Stream<SQSMessage> streamEvent(SQSEvent event, FunqyAmazonConfig amazonConfig) {
        if (event == null) {
            return Stream.empty();
        }
        return event.getRecords().stream();
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
    public SQSBatchResponse createResponse(List<String> failures, FunqyAmazonConfig amazonConfig) {
        if (!amazonConfig.advancedEventHandling().sqs().reportBatchItemFailures()) {
            return null;
        }
        return SQSBatchResponse.builder().withBatchItemFailures(
                failures.stream().map(id -> BatchItemFailure.builder().withItemIdentifier(id).build()).toList()).build();
    }

    @Override
    public Class<SQSMessage> getMessageClass() {
        return SQSMessage.class;
    }
}
