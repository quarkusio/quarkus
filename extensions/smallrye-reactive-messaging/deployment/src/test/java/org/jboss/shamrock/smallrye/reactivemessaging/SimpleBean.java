package org.jboss.shamrock.smallrye.reactivemessaging;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

@ApplicationScoped
public class SimpleBean {
    
    static final List<String> RESULT = new CopyOnWriteArrayList<>();
    
    @Outgoing("source")
    public PublisherBuilder<String> source() {
      return ReactiveStreams.of("hello", "with", "SmallRye", "reactive", "message");
    }

    @Incoming("source")
    @Outgoing("processed-a")
    public String toUpperCase(String payload) {
      return payload.toUpperCase();
    }

    @Incoming("processed-a")
    @Outgoing("processed-b")
    public PublisherBuilder<String> filter(PublisherBuilder<String> input) {
      return input.filter(item -> item.length() > 4);
    }

    @Incoming("processed-b")
    public void sink(String word) {
      RESULT.add(word);
    }

}
