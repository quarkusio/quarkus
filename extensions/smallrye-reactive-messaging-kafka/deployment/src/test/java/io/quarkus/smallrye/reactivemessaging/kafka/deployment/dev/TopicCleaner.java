package io.quarkus.smallrye.reactivemessaging.kafka.deployment.dev;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.kafka.client.runtime.KafkaAdminClient;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class TopicCleaner {

    @Inject
    KafkaAdminClient adminClient;

    void startup(@Observes StartupEvent event) {
        adminClient.deleteTopic("prices");
    }
}
