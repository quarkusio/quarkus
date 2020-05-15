package io.quarkus.it.amazon.sns;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.it.amazon.sqs.SqsQueueManager.Queue;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;

@Singleton
public class SnsManager {
    @Inject
    SnsClient sync;

    @Inject
    SnsAsyncClient async;

    private Map<String, String> topics = new HashMap<>();

    public void createTopic(String topicName) {
        CreateTopicResponse topic = sync.createTopic(t -> t.name(topicName));
        topics.put(topicName, topic.topicArn());
    }

    public void deleteTopic(String topicName) {
        sync.deleteTopic(t -> t.topicArn(topics.get(topicName)));
    }

    public String getTopicArn(String topicName) {
        return topics.get(topicName);
    }

    public String subscribe(String topicName, Queue queueSubscriber) {
        return sync
                .subscribe(s -> s.topicArn(getTopicArn(topicName)).protocol("sqs")
                        .endpoint(queueSubscriber.getQueueArn()))
                .subscriptionArn();
    }

    public String publishSync(String topicName, String message) {
        return sync.publish(p -> p.topicArn(getTopicArn(topicName)).message(message)).messageId();
    }

    public CompletionStage<String> publishAsync(String topicName, String message) {
        return async.publish(p -> p.topicArn(getTopicArn(topicName)).message(message))
                .thenApply(m -> m.messageId());
    }
}
