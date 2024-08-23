package io.quarkus.smallrye.reactivemessaging.mutiny;

import java.time.Duration;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Channel;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class MultiStringBean {
    @Channel(StringProducer.STRING_STREAM)
    Multi<String> strings;

    public List<String> getStrings(Duration duration) {
        return strings.collect()
                .asList()
                .await()
                .atMost(duration);
    }
}
