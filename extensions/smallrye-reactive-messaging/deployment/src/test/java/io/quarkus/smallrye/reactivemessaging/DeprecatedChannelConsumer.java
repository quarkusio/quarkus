package io.quarkus.smallrye.reactivemessaging;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.reactivex.Flowable;
import io.smallrye.reactive.messaging.annotations.Channel;

@SuppressWarnings("deprecation")
@ApplicationScoped
public class DeprecatedChannelConsumer {

    @Inject
    @Channel("source")
    Flowable<Message<String>> sourceStream;

    public List<String> consume() {
        return Flowable.fromPublisher(sourceStream)
                .map(Message::getPayload)
                .toList()
                .blockingGet();
    }

}
