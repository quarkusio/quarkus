package io.quarkus.smallrye.reactivemessaging.channels;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.annotations.Channel;

@ApplicationScoped
public class DeprecatedChannelConsumer {

    @Inject
    @Channel("source-channel")
    Publisher<Message<String>> sourceStream;

    public List<String> consume() {
        return Multi.createFrom().publisher(sourceStream)
                .onItem().transform(Message::getPayload)
                .collect().asList()
                .await().indefinitely();
    }

    @Outgoing("source-channel")
    public PublisherBuilder<String> source() {
        return ReactiveStreams.of("hello", "with", "SmallRye", "reactive", "message");
    }

}
