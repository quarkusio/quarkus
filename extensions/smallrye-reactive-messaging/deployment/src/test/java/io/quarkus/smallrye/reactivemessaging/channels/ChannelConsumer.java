package io.quarkus.smallrye.reactivemessaging.channels;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import io.reactivex.Flowable;

@ApplicationScoped
public class ChannelConsumer {

    @Inject
    @Channel("source-channel")
    Flowable<Message<String>> sourceStream;

    public List<String> consume() {
        return Flowable.fromPublisher(sourceStream)
                .map(Message::getPayload)
                .toList()
                .blockingGet();
    }

    @Outgoing("source-channel")
    public PublisherBuilder<String> source() {
        return ReactiveStreams.of("hello", "with", "SmallRye", "reactive", "message");
    }

}
