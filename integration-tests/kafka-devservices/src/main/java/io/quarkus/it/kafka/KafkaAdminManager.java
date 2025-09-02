package io.quarkus.it.kafka;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.TopicDescription;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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

    @PostConstruct
    void create() {
        admin = createAdmin(bs);
    }

    @PreDestroy
    void cleanup() {
        admin.close();
    }

    public int partitions(String topic) {

        TopicDescription topicDescription;
        try {
            Map<String, TopicDescription> partitions = admin.describeTopics(Collections.singletonList(topic))
                    .allTopicNames().get(2000, TimeUnit.MILLISECONDS);
            topicDescription = partitions.get(topic);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        if (topicDescription == null) {
            throw new IllegalArgumentException("Topic doesn't exist: " + topic);
        }
        return topicDescription.partitions().size();
    }

    int port() throws InterruptedException, ExecutionException {
        return admin.describeCluster().controller().get().port();
    }

    String image() throws InterruptedException, ExecutionException {
        // By observation, the red panda does not return anything for the supported features call
        // It would be nice to have a more robust check, but hopefully this fragile check is good enough
        boolean isRedPanda = admin.describeFeatures().featureMetadata().get().supportedFeatures().size() == 0;
        return isRedPanda ? "redpanda" : "kafka-native";
    }

}
