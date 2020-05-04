package io.quarkus.it.amazon.sqs;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

@Singleton
public class SqsQueueManager {

    @Inject
    SqsClient client;

    private Map<String, Queue> queues = new HashMap<>();

    public Queue createQueue(String queueName) {
        CreateQueueResponse queueResp = client.createQueue(q -> q.queueName(queueName));
        GetQueueAttributesResponse queueAttributes = client
                .getQueueAttributes(a -> a.queueUrl(queueResp.queueUrl()).attributeNames(QueueAttributeName.QUEUE_ARN));

        Queue queue = new SqsQueueManager.Queue(queueResp.queueUrl(),
                queueAttributes.attributes().get(QueueAttributeName.QUEUE_ARN));
        queues.put(queueName, queue);
        return queue;
    }

    public void deleteQueue(String queueName) {
        client.deleteQueue(q -> q.queueUrl(queues.get(queueName).queueUrl));
    }

    public Queue getQueue(String queueName) {
        return queues.get(queueName);
    }

    public static class Queue {
        private String queueUrl;
        private String queueArn;

        public Queue(String queueUrl, String queueArn) {
            this.queueUrl = queueUrl;
            this.queueArn = queueArn;
        }

        public String getQueueUrl() {
            return queueUrl;
        }

        public String getQueueArn() {
            return queueArn;
        }
    }
}
