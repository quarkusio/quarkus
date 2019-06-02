package io.quarkus.it.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.Stores;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
@Path("/kafkastreams")
public class KafkaStreamsPipeline {

    private KafkaStreams streams;

    private ExecutorService executor;

    void onStart(@Observes StartupEvent ev) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-test-pipeline");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, 500);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        StreamsBuilder builder = new StreamsBuilder();

        JsonObjectSerde jsonNodeSerde = new JsonObjectSerde();
        KTable<Integer, JsonObject> categories = builder.table(
                "streams-test-categories",
                Consumed.with(Serdes.Integer(), jsonNodeSerde));

        KStream<Integer, JsonObject> customers = builder
                .stream("streams-test-customers", Consumed.with(Serdes.Integer(), jsonNodeSerde))
                .selectKey((k, v) -> v.getJsonNumber("category").intValue())
                .join(
                        categories,
                        (v1, v2) -> {
                            JsonObjectBuilder target = Json.createObjectBuilder();
                            v1.forEach(target::add);
                            target.add("category", v2);
                            return target.build();
                        },
                        Joined.with(Serdes.Integer(), jsonNodeSerde, null));

        KeyValueBytesStoreSupplier storeSupplier = Stores.inMemoryKeyValueStore("countstore");
        customers.groupByKey()
                .count(Materialized.<Integer, Long> as(storeSupplier));

        customers.selectKey((k, v) -> v.getJsonNumber("id").intValue())
                .to("streams-test-customers-processed", Produced.with(Serdes.Integer(), jsonNodeSerde));

        streams = new KafkaStreams(builder.build(), props);

        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            waitForTopicsToBeCreated("localhost:19092");
            streams.start();
        });
    }

    private ReadOnlyKeyValueStore<Integer, Long> getCountstore() {
        while (true) {
            try {
                return streams.store("countstore", QueryableStoreTypes.keyValueStore());
            } catch (InvalidStateStoreException e) {
                // ignore, store not ready yet
            }
        }
    }

    @POST
    @Path("/stop")
    public void stop() {
        streams.close();
    }

    @GET
    @Path("/category/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Long getCategory(@PathParam("id") int id) {
        return getCountstore().get(id);
    }

    private void waitForTopicsToBeCreated(String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(config)) {
            AtomicBoolean topicsCreated = new AtomicBoolean(false);

            while (topicsCreated.get() == false) {
                ListTopicsResult topics = adminClient.listTopics();
                topics.names().whenComplete((t, e) -> {
                    if (e != null) {
                        throw new RuntimeException(e);
                    } else if (t.contains("streams-test-categories") && t.contains("streams-test-customers")) {
                        topicsCreated.set(true);
                    }
                });

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
