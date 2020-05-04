package io.quarkus.it.amazon.sqs;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/sqs")
public class SqsResource {

    @Inject
    SqsQueueManager queueMngr;

    @Inject
    SqsProducerManager producer;

    @Inject
    SqsConsumerManager consumer;

    @Path("queue/{queueName}")
    @POST
    public void createQueue(@PathParam("queueName") String queueName) {
        queueMngr.createQueue(queueName);
    }

    @Path("queue/{queueName}")
    @DELETE
    public void deleteQueue(@PathParam("queueName") String queueName) {
        queueMngr.deleteQueue(queueName);
    }

    @Path("sync/{queueName}")
    @POST
    @Produces(TEXT_PLAIN)
    public String sendSyncMessage(@PathParam("queueName") String queueName, @QueryParam("msg") String message) {
        return producer.sendSync(queueName, message);
    }

    @Path("sync/{queueName}")
    @GET
    @Produces(TEXT_PLAIN)
    public String getSyncMessages(@PathParam("queueName") String queueName) {
        return consumer.receiveSync(queueName).stream().collect(Collectors.joining(" "));
    }

    @Path("async/{queueName}")
    @POST
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> sendAsyncMessage(@PathParam("queueName") String queueName,
            @QueryParam("msg") String message) {
        return producer.sendAsync(queueName, message);
    }

    @Path("async/{queueName}")
    @GET
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> getAsyncMessages(@PathParam("queueName") String queueName) {
        return consumer.receiveAsync(queueName).thenApply(l -> l.stream().collect(Collectors.joining(" ")));
    }
}
