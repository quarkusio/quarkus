package io.quarkus.it.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.reactive.messaging.kafka.reply.KafkaRequestReply;

@ApplicationScoped
@Path("/kafka")
public class KafkaRequest {

    @Channel("request-reply")
    KafkaRequestReply<Integer, String> requestReply;

    @POST
    @Path("/req-rep")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String post(Integer request) {
        return requestReply.request(request).await().indefinitely();
    }

    @Incoming("request")
    @Outgoing("rep")
    public String process(int request) {
        return "reply-" + request;
    }

}
