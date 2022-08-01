package io.quarkus.kafka.client.runtime.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Singleton;

import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.Node;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.kafka.client.runtime.KafkaAdminClient;
import io.quarkus.kafka.client.runtime.ui.model.response.KafkaClusterInfo;
import io.quarkus.kafka.client.runtime.ui.model.response.KafkaInfo;
import io.quarkus.kafka.client.runtime.ui.model.response.KafkaNode;
import io.quarkus.kafka.client.runtime.ui.model.response.KafkaTopic;

@Singleton
public class KafkaUiUtils {

    private final KafkaAdminClient kafkaAdminClient;

    private final KafkaTopicClient kafkaTopicClient;

    private final ObjectMapper objectMapper;

    public KafkaUiUtils(KafkaAdminClient kafkaAdminClient, KafkaTopicClient kafkaTopicClient, ObjectMapper objectMapper) {
        this.kafkaAdminClient = kafkaAdminClient;
        this.kafkaTopicClient = kafkaTopicClient;
        this.objectMapper = objectMapper;
    }

    public KafkaInfo getKafkaInfo() throws ExecutionException, InterruptedException {
        var clusterInfo = getClusterInfo();
        var broker = clusterInfo.getController().asFullNodeName();
        var topics = getTopics();
        return new KafkaInfo(broker, clusterInfo, topics);
    }

    public KafkaClusterInfo getClusterInfo() throws ExecutionException, InterruptedException {
        return clusterInfo(kafkaAdminClient.getCluster());
    }

    private KafkaNode kafkaNode(Node node) {
        return new KafkaNode(node.host(), node.port(), node.idString());
    }

    private KafkaClusterInfo clusterInfo(DescribeClusterResult dcr) throws InterruptedException, ExecutionException {
        var controller = kafkaNode(dcr.controller().get());
        var nodes = new ArrayList<KafkaNode>();
        for (var node : dcr.nodes().get()) {
            nodes.add(kafkaNode(node));
        }
        var aclOperations = dcr.authorizedOperations().get();

        var aclOperationsStr = new StringBuilder();
        if (aclOperations != null) {
            for (var operation : dcr.authorizedOperations().get()) {
                if (aclOperationsStr.length() == 0) {
                    aclOperationsStr.append(", ");
                }
                aclOperationsStr.append(operation.name());
            }
        } else {
            aclOperationsStr = new StringBuilder("NONE");
        }

        return new KafkaClusterInfo(
                dcr.clusterId().get(),
                controller,
                nodes,
                aclOperationsStr.toString());
    }

    public List<KafkaTopic> getTopics() throws InterruptedException, ExecutionException {
        var res = new ArrayList<KafkaTopic>();
        for (TopicListing tl : kafkaAdminClient.getTopics()) {
            res.add(kafkaTopic(tl));
        }
        return res;
    }

    private KafkaTopic kafkaTopic(TopicListing tl) throws ExecutionException, InterruptedException {
        var partitions = partitions(tl.name());
        return new KafkaTopic(
                tl.name(),
                tl.topicId().toString(),
                partitions.size(),
                tl.isInternal());
    }

    public Collection<Integer> partitions(String topicName) throws ExecutionException, InterruptedException {
        return kafkaTopicClient.partitions(topicName);
    }

    public KafkaAclInfo getAclInfo() throws InterruptedException, ExecutionException {
        var clusterInfo = clusterInfo(kafkaAdminClient.getCluster());
        var entries = new ArrayList<KafkaAclEntry>();
        //TODO: fix it after proper error message impl
        try {
            var acls = kafkaAdminClient.getAclInfo();
            for (var acl : acls) {
                var entry = new KafkaAclEntry(
                        acl.entry().operation().name(),
                        acl.entry().principal(),
                        acl.entry().permissionType().name(),
                        acl.pattern().toString());
                entries.add(entry);
            }
        } catch (Exception e) {
            // this mostly means that ALC controller is absent
        }
        return new KafkaAclInfo(
                clusterInfo.getId(),
                clusterInfo.getController().asFullNodeName(),
                clusterInfo.getAclOperations(),
                entries);
    }

    public String toJson(Object o) {
        String res;
        try {
            res = objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException ex) {
            //FIXME:
            res = "";
        }
        return res;
    }
}
