package io.quarkus.smallrye.reactivemessaging.channels;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class ChannelConsumer {

    @Inject
    @Channel("source-channel")
    Multi<Message<String>> sourceStream;

    public List<String> consume() {
        return sourceStream
                .onItem().transform(Message::getPayload)
                .collect().asList()
                .await().indefinitely();
    }

    @Outgoing("source-channel")
    public PublisherBuilder<String> source() {
        return ReactiveStreams.of("hello", "with", "SmallRye", "reactive", "message");
    }

}
