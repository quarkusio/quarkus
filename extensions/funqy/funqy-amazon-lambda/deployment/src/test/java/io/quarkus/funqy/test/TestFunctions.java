package io.quarkus.funqy.test;

import java.nio.charset.StandardCharsets;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import io.cloudevents.CloudEvent;
import io.quarkus.funqy.Funq;
import io.smallrye.mutiny.Uni;

public class TestFunctions {

    @Funq("item-function")
    public Uni<Void> itemFunction(Item item) {
        if (item.isThrowError()) {
            return Uni.createFrom().failure(new IllegalArgumentException("This is an expected error."));
        }
        return Uni.createFrom().voidItem();
    }

    @Funq("sqs-function")
    public Uni<Void> sqsFunction(SQSEvent.SQSMessage msg) {
        if (msg.getBody().contains("true")) {
            return Uni.createFrom().failure(new IllegalArgumentException("This is an expected error."));
        }
        return Uni.createFrom().voidItem();
    }

    @Funq("sns-function")
    public Uni<Void> snsFunction(SNSEvent.SNSRecord msg) {
        if (msg.getSNS().getMessage().contains("true")) {
            return Uni.createFrom().failure(new IllegalArgumentException("This is an expected error."));
        }
        return Uni.createFrom().voidItem();
    }

    @Funq("cloudevents-function")
    public Uni<Void> cloudEventsFunction(CloudEvent msg) {
        // Due to jackson deserialization the base64 decoding already happened.
        if (new String(msg.getData().toBytes(), StandardCharsets.UTF_8).contains("true")) {
            return Uni.createFrom().failure(new IllegalArgumentException("This is an expected error."));
        }
        return Uni.createFrom().voidItem();
    }

    @Funq("kinesis-function")
    public Uni<Void> kinesisFunction(KinesisEvent.Record msg) {
        // Due to jackson deserialization the base64 decoding already happened.
        if (StandardCharsets.UTF_8.decode(msg.getData()).toString().contains("true")) {
            return Uni.createFrom().failure(new IllegalArgumentException("This is an expected error."));
        }
        return Uni.createFrom().voidItem();
    }

    @Funq("dynamodb-function")
    public Uni<Void> dynamodbFunction(DynamodbEvent.DynamodbStreamRecord msg) {
        if (msg.getDynamodb().getNewImage().get("ThrowError").getBOOL()) {
            return Uni.createFrom().failure(new IllegalArgumentException("This is an expected error."));
        }
        return Uni.createFrom().voidItem();
    }
}
