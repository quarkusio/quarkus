package io.quarkus.funqy.lambda.event;

import java.io.IOException;
import java.util.List;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.cloudevents.SpecVersion;
import io.quarkus.funqy.lambda.config.FunqyAmazonBuildTimeConfig;
import io.quarkus.funqy.lambda.model.cloudevents.CloudEventV1;
import io.quarkus.funqy.lambda.model.kinesis.PipesKinesisEvent;

public class EventDeserializer extends JsonDeserializer<Object> {

    private final FunqyAmazonBuildTimeConfig buildTimeConfig;

    private ObjectReader funqyMethodObjReader;
    private ObjectMapper objectMapper;

    public EventDeserializer(FunqyAmazonBuildTimeConfig buildTimeConfig) {
        this.buildTimeConfig = buildTimeConfig;
    }

    @Override
    public Object deserialize(JsonParser jsonParser, DeserializationContext ctx)
            throws IOException {

        ObjectCodec codec = jsonParser.getCodec();

        TreeNode rootNode = codec.readTree(jsonParser);

        if (buildTimeConfig.advancedEventHandling().enabled() && (rootNode.isObject() || rootNode.isArray())) {

            if (rootNode.isObject()) {
                // object
                ObjectNode object = (ObjectNode) rootNode;

                if (object.has("Records") && object.get("Records").isArray()) {
                    // We need to look into the first record entry, to distinguish the different types.
                    for (JsonNode record : object.get("Records")) {
                        return deserializeEvent(jsonParser, record, object, codec);
                    }
                }
            } else {
                // array. This happens in case of AWS EventBridge usage and is also the only way to enable
                // CloudEvents usage
                ArrayNode array = (ArrayNode) rootNode;
                for (JsonNode record : array) {
                    return deserializeEvent(jsonParser, record, array, codec);
                }
            }
        }
        // We have no clue what it is. Fallback to serializing the output of the funqy method
        return deserializeFunqReturnType(rootNode);
    }

    public Object deserializeEvent(final JsonParser jsonParser, JsonNode record, JsonNode rootNode, ObjectCodec codec)
            throws IOException {

        Object eventClass = getEventType(record, rootNode);

        if (eventClass != null) {
            if (eventClass instanceof Class<?> clazz) {
                return objectMapper.convertValue(rootNode, clazz);
            } else if (eventClass instanceof TypeReference<?> typeReference) {
                return objectMapper.convertValue(rootNode, typeReference);
            }
        }

        return deserializeFunqReturnType(rootNode);
    }

    private Object getEventType(JsonNode record, JsonNode object) {
        String eventSource = getEventSource(record);

        if (eventSource == null) {
            eventSource = "default";
        }

        // See: https://docs.aws.amazon.com/lambda/latest/dg/lambda-services.html
        // and for Pipes: https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-pipes-event-source.html
        Class<?> eventType = null;
        switch (eventSource) {
            case "aws:sqs":
                if (object.isObject()) {
                    eventType = SQSEvent.class;
                } else {
                    // EventBridge Pipes
                    return new TypeReference<List<SQSEvent.SQSMessage>>() {
                    };
                }
                break;
            case "aws:sns":
                eventType = SNSEvent.class;
                break;
            case "aws:kinesis":
                // Exclude Kinesis time window events. This would require to send a responsible with a state.
                // This is very specific to AWS and maybe not the way funqy wants to handle things.
                if (object.isObject() && !object.has("window")) {
                    eventType = KinesisEvent.class;
                } else if (object.isArray()) {
                    // EventBridge Pipes
                    return new TypeReference<List<PipesKinesisEvent>>() {
                    };
                }
                break;
            case "aws:dynamodb":
                if (object.isObject()) {
                    eventType = DynamodbEvent.class;
                } else {
                    // EventBridge Pipes
                    return new TypeReference<List<DynamodbEvent.DynamodbStreamRecord>>() {
                    };
                }
                break;
            default:
                break;
        }
        if (eventType == null && isCloudEvent(record)) {
            return new TypeReference<List<CloudEventV1>>() {
            };
        }
        return eventType;
    }

    private boolean isCloudEvent(final JsonNode record) {
        // this is the best guess we can do. We check for required attributes:
        // https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/spec.md#required-attributes
        // A more tolerant way to check the type. We do not want to process to fail here. We can fall back to
        // the best guess parting
        return record.has("specversion") && record.get("specversion").isTextual() &&
                SpecVersion.V1.toString().equals(record.get("specversion").asText()) &&
                record.has("type");
    }

    private String getEventSource(JsonNode record) {
        if (record.has("eventSource") && record.get("eventSource").isTextual()) {
            return record.get("eventSource").asText();
        }

        // Unsure. In the AWS documentation SNS uses capital keys. I assume this is a mistake.
        if (record.has("EventSource") && record.get("EventSource").isTextual()) {
            return record.get("EventSource").asText();
        }
        return null;
    }

    private Object deserializeFunqReturnType(TreeNode node) throws IOException {
        if (funqyMethodObjReader != null) {
            return funqyMethodObjReader.readValue(node.traverse());
        }
        return null;
    }

    public void setFunqyMethodObjReader(ObjectReader funqyMethodObjReader) {
        this.funqyMethodObjReader = funqyMethodObjReader;
    }

    public void setObjectMapper(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
