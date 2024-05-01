package io.quarkus.funqy.lambda.event.dynamodb;

import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;

import io.quarkus.funqy.lambda.config.FunqyAmazonConfig;
import io.quarkus.funqy.lambda.event.EventHandler;

public class DynamoDbEventHandler implements EventHandler<DynamodbEvent, DynamodbStreamRecord, StreamsEventResponse> {

    @Override
    public Stream<DynamodbStreamRecord> streamEvent(DynamodbEvent event, FunqyAmazonConfig amazonConfig) {
        if (event == null) {
            return Stream.empty();
        }
        return event.getRecords().stream();
    }

    @Override
    public String getIdentifier(DynamodbStreamRecord message, FunqyAmazonConfig amazonConfig) {
        return message.getDynamodb().getSequenceNumber();
    }

    @Override
    public Supplier<InputStream> getBody(DynamodbStreamRecord message, FunqyAmazonConfig amazonConfig) {
        throw new IllegalStateException("""
                DynamoDB records are too specific. It is not supported to extract a message from them. \
                Use the DynamodbStreamRecord in your funq method, or use EventBridge Pipes with CloudEvents.
                """);
    }

    @Override
    public StreamsEventResponse createResponse(List<String> failures, FunqyAmazonConfig amazonConfig) {
        if (!amazonConfig.advancedEventHandling().dynamoDb().reportBatchItemFailures()) {
            return null;
        }
        return StreamsEventResponse.builder().withBatchItemFailures(
                failures.stream().map(id -> StreamsEventResponse.BatchItemFailure.builder()
                        .withItemIdentifier(id).build()).toList())
                .build();
    }

    @Override
    public Class<DynamodbStreamRecord> getMessageClass() {
        return DynamodbStreamRecord.class;
    }
}
