package io.quarkus.it.kafka.streams;

import java.util.List;

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

import io.quarkus.it.kafka.streams.KafkaStreamsPipeline.CurrentStateListener;

@ApplicationScoped
@Path("/kafkastreams")
public class KafkaStreamsEndpoint {

    @Inject
    KafkaStreams streams;

    @Inject
    CurrentStateListener stateListener;

    @Inject
    CdiProcessorTracker cdiProcessorTracker;

    private ReadOnlyKeyValueStore<Integer, Long> getCountstore() {
        while (true) {
            try {
                return streams.store(StoreQueryParameters.fromNameAndType("countstore", QueryableStoreTypes.keyValueStore()));
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
    public Long getCategory(@PathParam("id") int id) {
        return getCountstore().get(id);
    }

    @GET
    @Path("/state")
    public String state() {
        return stateListener.currentState.name();
    }

    @GET
    @Path("/cdi-processor/count")
    public int cdiProcessorCount() {
        return cdiProcessorTracker.getProcessedCount();
    }

    @GET
    @Path("/cdi-processor/values")
    public List<String> cdiProcessorValues() {
        return cdiProcessorTracker.getProcessedValues();
    }
}
