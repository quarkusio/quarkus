package io.quarkus.it.compose.devservices.kafka;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.Startup;

@ApplicationScoped
public class KafkaAdminManager {

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bs;

    private static AdminClient createAdmin(String kafkaBootstrapServers) {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        return AdminClient.create(properties);
    }

    AdminClient admin;

    @Startup
    void startup() throws ExecutionException, InterruptedException {
        admin = createAdmin(bs);
        if (!admin.listTopics().names().get().contains("test")) {
            admin.createTopics(List.of(new NewTopic("test", 2, (short) 1),
                    new NewTopic("test-consumer", 3, (short) 1))).all().get();
        }
    }

    @PreDestroy
    void cleanup() {
        admin.close();
    }

    public int partitions(String topic) {
        TopicDescription topicDescription;
        try {
            Map<String, TopicDescription> partitions = admin.describeTopics(Collections.singletonList(topic)).all()
                    .get(2000, TimeUnit.MILLISECONDS);
            topicDescription = partitions.get(topic);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        if (topicDescription == null) {
            throw new IllegalArgumentException("Topic doesn't exist: " + topic);
        }
        return topicDescription.partitions().size();
    }

}
