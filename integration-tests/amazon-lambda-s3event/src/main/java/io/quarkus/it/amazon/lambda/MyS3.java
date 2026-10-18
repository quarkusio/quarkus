package io.quarkus.it.amazon.lambda;

import org.junit.jupiter.api.Assertions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;

public class MyS3 implements RequestHandler<S3Event, String> {

    @Override
    public String handleRequest(S3Event event, Context context) {
        Assertions.assertEquals(1, event.getRecords().size());
        S3EventNotification.S3EventNotificationRecord record = event.getRecords().get(0);
        Assertions.assertEquals("2.0", record.getEventVersion());
        Assertions.assertEquals("aws:s3", record.getEventSource());
        Assertions.assertEquals("us-west-2", record.getAwsRegion());
        Assertions.assertEquals("1970-01-01T00:00:00.000Z", record.getEventTime().toString());
        Assertions.assertEquals("ObjectCreated:Put", record.getEventName());
        Assertions.assertEquals("AIDAJDPLRKLG7UEXAMPLE", record.getUserIdentity().getPrincipalId());
        Assertions.assertEquals("127.0.0.1", record.getRequestParameters().getSourceIPAddress());
        Assertions.assertEquals("C3D13FE58DE4C810", record.getResponseElements().getxAmzRequestId());
        Assertions.assertEquals("FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANOjpD",
                record.getResponseElements().getxAmzId2());

        Assertions.assertEquals("1.0", record.getS3().getS3SchemaVersion());
        Assertions.assertEquals("testConfigRule", record.getS3().getConfigurationId());
        Assertions.assertEquals("sourcebucket", record.getS3().getBucket().getName());
        Assertions.assertEquals("arn:aws:s3:::sourcebucket", record.getS3().getBucket().getArn());
        Assertions.assertEquals("A3NL1KOZZKExample", record.getS3().getBucket().getOwnerIdentity().getPrincipalId());
        Assertions.assertEquals("HappyFace.jpg", record.getS3().getObject().getKey());
        Assertions.assertEquals(1024, record.getS3().getObject().getSizeAsLong());
        Assertions.assertEquals("d41d8cd98f00b204e9800998ecf8427e", record.getS3().getObject().geteTag());
        Assertions.assertEquals("096fKKXTRTtl3on89fVO.nfljtsv6qko", record.getS3().getObject().getVersionId());
        return "Ok";
    }
}
