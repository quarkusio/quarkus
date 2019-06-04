package io.quarkus.it.kafka;

import static org.awaitility.Awaitility.await;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    private static final String CATEGORIES_TOPIC_NAME = "streams-test-categories";
    private static final String CUSTOMERS_TOPIC_NAME = "streams-test-customers";

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
                CATEGORIES_TOPIC_NAME,
                Consumed.with(Serdes.Integer(), jsonNodeSerde));

        KStream<Integer, JsonObject> customers = builder
                .stream(CUSTOMERS_TOPIC_NAME, Consumed.with(Serdes.Integer(), jsonNodeSerde))
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
            await().until(topicsCreated(adminClient, CATEGORIES_TOPIC_NAME, CUSTOMERS_TOPIC_NAME));
        }
    }

    private Callable<Boolean> topicsCreated(AdminClient adminClient, String... expectedTopics) {
        return new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                ListTopicsResult topics = adminClient.listTopics();
                Set<String> topicNames = topics.names().get(10, TimeUnit.SECONDS);

                return topicNames.containsAll(Arrays.asList(expectedTopics));
            }
        };
  }
}
