package io.quarkus.it.kafka.streams.norocksdb;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

@ApplicationScoped
@Path("/wordcount")
public class WordCountEndpoint {

    @Inject
    KafkaStreams streams;

    @GET
    @Path("/{word}")
    public Long getCount(@PathParam("word") String word) {
        return getStore().get(word);
    }

    @POST
    @Path("/stop")
    public void stop() {
        streams.close();
    }

    private ReadOnlyKeyValueStore<String, Long> getStore() {
        while (true) {
            try {
                return streams.store(
                        StoreQueryParameters.fromNameAndType("word-count-store", QueryableStoreTypes.keyValueStore()));
            } catch (InvalidStateStoreException e) {
                // ignore, store not ready yet
            }
        }
    }
}
