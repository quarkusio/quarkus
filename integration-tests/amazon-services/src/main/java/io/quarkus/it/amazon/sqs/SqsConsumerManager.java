package io.quarkus.it.amazon.sqs;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@ApplicationScoped
public class SqsConsumerManager {

    @Inject
    SqsQueueManager queueManager;

    @Inject
    SqsClient sync;

    @Inject
    SqsAsyncClient async;

    public List<String> receiveSync(String queueName) {
        return sync.receiveMessage(m -> m.queueUrl(queueManager.getQueue(queueName).getQueueUrl()).maxNumberOfMessages(10))
                .messages().stream()
                .map(msg -> msg.body()).collect(Collectors.toList());
    }

    public CompletionStage<List<String>> receiveAsync(String queueName) {
        return async.receiveMessage(m -> m.queueUrl(queueManager.getQueue(queueName).getQueueUrl()).maxNumberOfMessages(10))
                .thenApply(b -> b.messages().stream().map(msg -> msg.body()).collect(Collectors.toList()));
    }

}
