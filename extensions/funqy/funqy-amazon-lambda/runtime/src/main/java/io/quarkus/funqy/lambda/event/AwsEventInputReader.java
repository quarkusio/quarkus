package io.quarkus.funqy.lambda.event;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.cloudevents.SpecVersion;
import io.quarkus.amazon.lambda.runtime.LambdaInputReader;
import io.quarkus.funqy.lambda.config.FunqyAmazonBuildTimeConfig;
import io.quarkus.funqy.lambda.model.cloudevents.CloudEventV1;
import io.quarkus.funqy.lambda.model.kinesis.PipesKinesisEvent;

public class AwsEventInputReader implements LambdaInputReader<Object> {

    private static final Logger log = Logger.getLogger(AwsEventInputReader.class);

    private static final String SQS_EVENT_SOURCE = "aws:sqs";
    private static final String SNS_EVENT_SOURCE = "aws:sns";
    private static final String KINESIS_EVENT_SOURCE = "aws:kinesis";
    private static final String DYNAMODB_EVENT_SOURCE = "aws:dynamodb";

    final ObjectMapper mapper;
    final FunqyAmazonBuildTimeConfig amazonBuildTimeConfig;
    final ObjectReader reader;

    public AwsEventInputReader(ObjectMapper mapper, ObjectReader reader,
            FunqyAmazonBuildTimeConfig amazonBuildTimeConfig) {
        // configure the mapper for advanced event handling
        final SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(Date.class, new DateDeserializer());
        mapper.registerModule(simpleModule);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        this.mapper = mapper;
        this.amazonBuildTimeConfig = amazonBuildTimeConfig;
        this.reader = reader;
    }

    @Override
    public Object readValue(InputStream is) throws IOException {
        try {
            return safelyReadValue(is);
        } catch (JacksonException e) {
            // to make sure that we do not expose too many details about the issue in the lambda response
            // we have some special treatment for jackson related issues
            throw new IllegalArgumentException("Could not deserialize the provided message.", e);
        }
    }

    private Object safelyReadValue(final InputStream is) throws IOException {
        final JsonNode rootNode = mapper.readTree(is);

        if (amazonBuildTimeConfig.advancedEventHandling().enabled()) {
            if (rootNode.isObject() || rootNode.isArray()) {
                if (rootNode.isObject()) {
                    // object
                    ObjectNode object = (ObjectNode) rootNode;

                    if (object.has("Records") && object.get("Records").isArray()) {
                        // We need to look into the first record entry, to distinguish the different types.
                        for (JsonNode record : object.get("Records")) {
                            return deserializeEvent(record, object);
                        }
                    }
                } else {
                    // array. This happens in case of AWS EventBridge usage, and is also the only way to enable
                    // CloudEvents usage
                    ArrayNode array = (ArrayNode) rootNode;
                    for (JsonNode record : array) {
                        return deserializeEvent(record, array);
                    }
                }
            }
            log.debug("Could not detect event type. Try to deserialize to funqy method type");
        } else {
            log.debug("Advanced event handling disabled. Try to deserialize to funqy method type");
        }
        // We could not identify the event based on the content. Fallback to deserializing the funq method input type.
        return deserializeFunqReturnType(rootNode);
    }

    public Object deserializeEvent(JsonNode record, JsonNode rootNode) throws IOException {
        Object eventClass = getEventType(record, rootNode);

        log.debugv("Detected event class: {0}", eventClass);

        if (eventClass != null) {
            if (eventClass instanceof Class<?> clazz) {
                return mapper.convertValue(rootNode, clazz);
            } else if (eventClass instanceof TypeReference<?> typeReference) {
                return mapper.convertValue(rootNode, typeReference);
            }
        }

        log.debug("Could not detect event type. Try to deserialize to funqy method type");

        // We could not identify the event based on the content. Fallback to deserializing the funq method input type.
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
            case SQS_EVENT_SOURCE:
                if (object.isObject()) {
                    eventType = SQSEvent.class;
                } else if (object.isArray()) {
                    // EventBridge Pipes
                    return new TypeReference<List<SQSEvent.SQSMessage>>() {
                    };
                }
                break;
            case SNS_EVENT_SOURCE:
                eventType = SNSEvent.class;
                break;
            case KINESIS_EVENT_SOURCE:
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
            case DYNAMODB_EVENT_SOURCE:
                if (object.isObject()) {
                    eventType = DynamodbEvent.class;
                } else if (object.isArray()) {
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
        // A more tolerant way to check the type. We do not want the process to fail. We can fall back to
        // the best guess logic.
        return record.has("specversion") && record.get("specversion").isTextual() &&
                SpecVersion.V1.toString().equals(record.get("specversion").asText()) &&
                record.has("type");
    }

    private String getEventSource(JsonNode record) {
        if (record.has("eventSource") && record.get("eventSource").isTextual()) {
            return record.get("eventSource").asText();
        }

        // Unsure. In the AWS SNS documentation the key starts with a capital letter. I assume this is a mistake,
        // but it should not hurt to be that tolerant as well.
        if (record.has("EventSource") && record.get("EventSource").isTextual()) {
            return record.get("EventSource").asText();
        }
        return null;
    }

    private Object deserializeFunqReturnType(TreeNode node) throws IOException {
        if (reader != null) {
            return reader.readValue(node.traverse());
        }
        return null;
    }
}
