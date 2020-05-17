package io.quarkus.kafka.streams.runtime;

import static org.apache.kafka.common.serialization.Serdes.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.regex.Pattern;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.junit.jupiter.api.Test;

public class TopologyHelperTest {

    @Test
    public void staticTopicsShouldBeDiscovered() {
        Topology topology = new Topology();
        topology.addSource("source", "topic1", "topic2");
        topology.addSink("sink", "topic3", "source");

        Set<String> topics = TopologyHelper.discoverStaticTopics(topology);

        assertThat(topics).containsOnly("topic1", "topic2", "topic3");
    }

    @Test
    public void dynamicTopicsShouldBeIgnored() {
        Topology topology = new Topology();
        topology.addSource("source", Pattern.compile("topic.*"));
        topology.addSink("sink", (key, value, recordContext) -> "topic1", "source");

        Set<String> topics = TopologyHelper.discoverStaticTopics(topology);

        assertThat(topics).isEmpty();
    }

    @Test
    public void internalSubTopologyTopicsShouldBeIgnored() {
        StreamsBuilder builder = new StreamsBuilder();
        KTable<String, String> ktable = builder.table(
                "topic1",
                Consumed.with(String(), String()));

        KStream<String, String> stream = builder
                .stream("topic2", Consumed.with(String(), String()))
                .selectKey((id, value) -> id)
                .join(
                        ktable,
                        (value1, value2) -> {
                            return value1;
                        },
                        Joined.with(String(), String(), String()));

        stream.to("topic3", Produced.with(new StringSerde(), new StringSerde()));

        Set<String> topics = TopologyHelper.discoverStaticTopics(builder.build());

        assertThat(topics).containsOnly("topic1", "topic2", "topic3");
    }
}
