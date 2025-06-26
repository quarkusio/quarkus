package io.quarkus.kafka.streams.runtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.streams.Topology;
import org.junit.jupiter.api.Test;

class KafkaStreamsTopologyManagerTest {

    @Test
    void waitForTopicsWhenCheckWhenIsDisabledShouldNotInteractWithAdminClient() throws Exception {
        AdminClient adminClient = mock(AdminClient.class);
        KafkaStreamsRuntimeConfig config = mock(KafkaStreamsRuntimeConfig.class);
        Topology topology = mock(Topology.class);

        // GIVEN a KafkaStreamsTopologyManager with topics check disabled
        when(config.topicsTimeout()).thenReturn(Duration.ZERO);
        KafkaStreamsTopologyManager manager = new KafkaStreamsTopologyManager(
                adminClient,
                topology,
                config);

        // WHEN waiting for topics to be created
        manager.waitForTopicsToBeCreated();

        // THEN it should not interact with the admin client
        verify(adminClient, never()).listTopics();
    }

    @Test
    void waitForTopicsWhenCheckWhenIsEnabledShouldInteractWithAdminClient() throws Exception {
        AdminClient adminClient = mock(AdminClient.class);
        KafkaStreamsRuntimeConfig config = mock(KafkaStreamsRuntimeConfig.class);
        Topology topology = mock(Topology.class);
        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> namesFuture = mock(KafkaFuture.class);

        // GIVEN a KafkaStreamsTopologyManager with topics check enabled expecting topic `topic1`
        // AND an admin client that returns `topic1` immediately
        String expectedTopic = "topic1";
        when(config.topicsTimeout()).thenReturn(Duration.ofSeconds(30));
        when(config.topics()).thenReturn(Optional.of(List.of(expectedTopic)));

        when(adminClient.listTopics()).thenReturn(listTopicsResult);
        when(listTopicsResult.names()).thenReturn(namesFuture);
        when(namesFuture.get(anyLong(), any())).thenReturn(Set.of(expectedTopic));

        KafkaStreamsTopologyManager manager = new KafkaStreamsTopologyManager(
                adminClient,
                topology,
                config);

        // WHEN waiting for topics to be created
        manager.waitForTopicsToBeCreated();

        // THEN it should interact with the admin client
        verify(adminClient, times(1)).listTopics();
    }
}