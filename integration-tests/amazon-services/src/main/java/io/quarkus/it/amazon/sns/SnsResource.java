package io.quarkus.it.amazon.sns;

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

import io.quarkus.it.amazon.sqs.SqsConsumerManager;
import io.quarkus.it.amazon.sqs.SqsQueueManager;

@Path("/sns")
public class SnsResource {
    @Inject
    SnsManager snsManager;

    @Inject
    SqsQueueManager queueManager;

    @Inject
    SqsConsumerManager queueConsumer;

    @Path("topics/{topicName}")
    @POST
    public void createTopicAndSubscribeQueue(@PathParam("topicName") String topicName) {
        snsManager.createTopic(topicName);
        snsManager.subscribe(topicName, queueManager.createQueue(topicName));
    }

    @Path("topics/{topicName}")
    @DELETE
    public void deleteQueue(@PathParam("topicName") String topicName) {
        snsManager.deleteTopic(topicName);
        queueManager.deleteQueue(topicName);
    }

    @Path("topics/{topicName}")
    @GET
    @Produces(TEXT_PLAIN)
    public String readPublishedMessages(@PathParam("topicName") String topicName) {
        return queueConsumer.receiveSync(topicName).stream().collect(Collectors.joining(" "));
    }

    @Path("sync/publish/{topicName}")
    @POST
    @Produces(TEXT_PLAIN)
    public String publishSync(@PathParam("topicName") String topicName, @QueryParam("msg") String message) {
        return snsManager.publishSync(topicName, message);
    }

    @Path("async/publish/{topicName}")
    @POST
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> publishAsync(@PathParam("topicName") String topicName, @QueryParam("msg") String message) {
        return snsManager.publishAsync(topicName, message);
    }
}
