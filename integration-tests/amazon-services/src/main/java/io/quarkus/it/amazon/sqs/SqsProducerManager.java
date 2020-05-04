package io.quarkus.it.amazon.sqs;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@ApplicationScoped
public class SqsProducerManager {

    @Inject
    SqsQueueManager queueManager;

    @Inject
    SqsClient sync;

    @Inject
    SqsAsyncClient async;

    public String sendSync(String queueName, String message) {
        return sync.sendMessage(m -> m.queueUrl(queueManager.getQueue(queueName).getQueueUrl()).messageBody(message))
                .messageId();
    }

    public CompletionStage<String> sendAsync(String queueName, String message) {
        return async.sendMessage(m -> m.queueUrl(queueManager.getQueue(queueName).getQueueUrl()).messageBody(message))
                .thenApply(r -> r.messageId());
    }

}
