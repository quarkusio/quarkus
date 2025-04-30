package io.quarkus.funqy.lambda.event.sns;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;

import io.quarkus.funqy.lambda.config.FunqyAmazonConfig;
import io.quarkus.funqy.lambda.event.EventHandler;

public class SnsEventHandler implements EventHandler<SNSEvent, SNSRecord, Void> {

    @Override
    public Stream<SNSRecord> streamEvent(SNSEvent event, FunqyAmazonConfig amazonConfig) {
        if (event == null) {
            return Stream.empty();
        }
        return event.getRecords().stream();
    }

    @Override
    public String getIdentifier(SNSRecord message, FunqyAmazonConfig amazonConfig) {
        return message.getSNS().getMessageId();
    }

    @Override
    public Supplier<InputStream> getBody(SNSRecord message, FunqyAmazonConfig amazonConfig) {
        if (message.getSNS() == null) {
            return ByteArrayInputStream::nullInputStream;
        }
        return () -> new ByteArrayInputStream(message.getSNS().getMessage().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Void createResponse(List<String> failures, FunqyAmazonConfig amazonConfig) {
        // SNS does not support batch item failures. We return nothing, which results in no response
        return null;
    }

    @Override
    public Class<SNSRecord> getMessageClass() {
        return SNSRecord.class;
    }
}
