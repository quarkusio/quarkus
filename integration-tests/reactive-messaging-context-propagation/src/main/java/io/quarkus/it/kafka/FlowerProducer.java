package io.quarkus.it.kafka;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Channel;

import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.MutinyEmitter;

@Path("/flowers")
public class FlowerProducer {

    List<String> received = new CopyOnWriteArrayList<>();

    @Channel("flowers-out")
    MutinyEmitter<String> emitter;

    @Inject
    RequestBean reqBean;

    @POST
    @Path("/produce")
    @Consumes(MediaType.TEXT_PLAIN)
    public void produce(String flower) {
        reqBean.setName(flower);
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        emitter.sendAndAwait(flower);
    }

    void addReceived(String flower) {
        received.add(flower);
    }

    public List<String> getReceived() {
        return received;
    }

    @GET
    @Path("/received")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> received() {
        return received;
    }
}
