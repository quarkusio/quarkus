package io.quarkus.amazon.lambda.runtime.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.amazon.lambda.runtime.LambdaInputReader;

public class S3EventInputReader implements LambdaInputReader<S3Event> {
    final ObjectMapper mapper;

    public S3EventInputReader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public S3Event readValue(InputStream is) throws IOException {
        JsonNode json = mapper.readTree(is);
        JsonNode records = json.get("Records");
        if (records == null || !records.isArray()) {
            return new S3Event(Collections.EMPTY_LIST);
        }
        List<S3EventNotification.S3EventNotificationRecord> list = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            JsonNode record = records.get(i);
            String awsRegion = JacksonUtil.getText("awsRegion", record);
            String eventName = JacksonUtil.getText("eventName", record);
            String eventSource = JacksonUtil.getText("eventSource", record);
            String eventTime = JacksonUtil.getText("eventTime", record);
            String eventVersion = JacksonUtil.getText("eventVersion", record);
            JsonNode params = record.get("requestParameters");
            S3EventNotification.RequestParametersEntity requestParameters = null;
            if (params != null) {
                requestParameters = new S3EventNotification.RequestParametersEntity(
                        JacksonUtil.getText("sourceIPAddress", params));
            }
            JsonNode elems = record.get("responseElements");
            S3EventNotification.ResponseElementsEntity responseElements = null;
            if (elems != null) {
                String requestId = JacksonUtil.getText("x-amz-request-id", elems);
                String id = JacksonUtil.getText("x-amz-id-2", elems);
                responseElements = new S3EventNotification.ResponseElementsEntity(id, requestId);
            }
            JsonNode userIdentity = record.get("userIdentity");
            S3EventNotification.UserIdentityEntity userId = null;
            if (userIdentity != null) {
                String principalId = JacksonUtil.getText("principalId", userIdentity);
                userId = new S3EventNotification.UserIdentityEntity(principalId);
            }

            JsonNode s3 = record.get("s3");
            S3EventNotification.S3Entity s3Entity = null;
            if (s3 != null) {
                String configurationId = JacksonUtil.getText("configurationId", s3);
                String schemaVersion = JacksonUtil.getText("s3SchemaVersion", s3);
                JsonNode bucketNode = s3.get("bucket");
                S3EventNotification.S3BucketEntity bucket = null;
                if (bucketNode != null) {
                    String name = JacksonUtil.getText("name", bucketNode);
                    JsonNode ownerIdentity = bucketNode.get("ownerIdentity");
                    S3EventNotification.UserIdentityEntity owner = null;
                    if (ownerIdentity != null) {
                        String principalId = JacksonUtil.getText("principalId", ownerIdentity);
                        owner = new S3EventNotification.UserIdentityEntity(principalId);
                    }
                    String arn = JacksonUtil.getText("arn", bucketNode);
                    bucket = new S3EventNotification.S3BucketEntity(name, owner, arn);
                }
                JsonNode object = s3.get("object");
                S3EventNotification.S3ObjectEntity obj = null;
                if (object != null) {
                    String key = JacksonUtil.getText("key", object);
                    Long size = JacksonUtil.getLong("size", object);
                    String eTag = JacksonUtil.getText("eTag", object);
                    String versionId = JacksonUtil.getText("versionId", object);
                    String sequencer = JacksonUtil.getText("sequencer", object);
                    obj = new S3EventNotification.S3ObjectEntity(key, size, eTag, versionId, sequencer);
                }
                s3Entity = new S3EventNotification.S3Entity(configurationId, bucket, obj, schemaVersion);
            }
            S3EventNotification.S3EventNotificationRecord r = new S3EventNotification.S3EventNotificationRecord(awsRegion,
                    eventName, eventSource, eventTime,
                    eventVersion, requestParameters,
                    responseElements, s3Entity, userId);
            list.add(r);
        }
        return new S3Event(list);
    }
}
