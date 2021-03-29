package io.quarkus.it.kafka.streams;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.kafka.streams.KafkaStreams;
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
    public Long getCategory(@PathParam("id") int id) {
        return getCountstore().get(id);
    }

    @GET
    @Path("/state")
    public String state() {
        return stateListener.currentState.name();
    }
}
