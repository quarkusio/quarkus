package io.quarkus.it.gcp.services;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;
import org.jboss.logging.Logger;

@Path("/pubsub")
public class PubSubResource {
    private static final Logger LOG = Logger.getLogger(PubSubResource.class);

    @ConfigProperty(name="quarkus.google.cloud.project-id") String projectId;

    private TopicName topicName;
    private Subscriber subscriber;

    @PostConstruct
    void init() throws IOException {
        topicName = TopicName.of(projectId, "test-topic");
        ProjectSubscriptionName subscriptionName = initTopicAndSubscription();

        // subscribe to PubSub
        MessageReceiver receiver =  (message, consumer) -> {
            LOG.infov("Got message {0}", message.getData().toStringUtf8());
            consumer.ack();
        };
        subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();;
        subscriber.startAsync().awaitRunning();
    }

    @PreDestroy
    void destroy() {
        if(subscriber != null){
            subscriber.stopAsync();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public void pubsub() throws IOException, InterruptedException {
        Publisher publisher = Publisher.newBuilder(topicName).build();
        try {
            ByteString data = ByteString.copyFromUtf8("my-message");
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
            ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
            ApiFutures.addCallback(messageIdFuture, new ApiFutureCallback<String>() {
                public void onSuccess(String messageId) {
                    LOG.infov("published with message id {0}", messageId);
                }

                public void onFailure(Throwable t) {
                    LOG.warnv("failed to publish: {0}", t);
                }
            }, MoreExecutors.directExecutor());
        } finally {
            publisher.shutdown();
            publisher.awaitTermination(1, TimeUnit.MINUTES);
        }
    }

    private ProjectSubscriptionName initTopicAndSubscription() throws IOException {
        try (TopicAdminClient topicAdminClient = TopicAdminClient.create()) {
            Iterable<Topic> topics = topicAdminClient.listTopics(ProjectName.of(projectId)).iterateAll();
            Optional<Topic> existing = StreamSupport.stream(topics.spliterator(), false)
                    .filter(topic -> topic.getName().equals(topicName.toString()))
                    .findFirst();
            if(!existing.isPresent()){
                topicAdminClient.createTopic(topicName.toString());
            }
        }
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, "test-subscription");
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
            Iterable<Subscription> subscriptions = subscriptionAdminClient.listSubscriptions(ProjectName.of(projectId)).iterateAll();
            Optional<Subscription> existing = StreamSupport.stream(subscriptions.spliterator(), false)
                    .filter(sub -> sub.getName().equals(subscriptionName.toString()))
                    .findFirst();
            if(!existing.isPresent()){
                subscriptionAdminClient.createSubscription(subscriptionName, topicName, PushConfig.getDefaultInstance(), 0);
            }
        }
        return subscriptionName;
    }
}