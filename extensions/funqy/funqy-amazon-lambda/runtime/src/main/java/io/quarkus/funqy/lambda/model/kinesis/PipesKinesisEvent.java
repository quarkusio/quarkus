package io.quarkus.funqy.lambda.model.kinesis;

import com.amazonaws.services.lambda.runtime.events.KinesisEvent;

/**
 * For some reason AWS decided to flatten the model in EventBridge pipes for Kinesis. So there is no additional
 * property called kinesis. We use the Record model and add the missing properties. Sadly I could not find a
 * dedicated model for Kinesis in Pipes. So it is a combination of
 * {@link com.amazonaws.services.lambda.runtime.events.KinesisEvent.KinesisEventRecord} and {@link KinesisEvent.Record}
 */
public class PipesKinesisEvent extends KinesisEvent.Record {

    private static final long serialVersionUID = 4365865918351932405L;

    private String eventSource;

    private String eventID;

    private String invokeIdentityArn;

    private String eventName;

    private String eventVersion;

    private String eventSourceARN;

    private String awsRegion;

    public PipesKinesisEvent setEventSource(final String eventSource) {
        this.eventSource = eventSource;
        return this;
    }

    public PipesKinesisEvent setEventID(final String eventID) {
        this.eventID = eventID;
        return this;
    }

    public PipesKinesisEvent setInvokeIdentityArn(final String invokeIdentityArn) {
        this.invokeIdentityArn = invokeIdentityArn;
        return this;
    }

    public PipesKinesisEvent setEventName(final String eventName) {
        this.eventName = eventName;
        return this;
    }

    public PipesKinesisEvent setEventVersion(final String eventVersion) {
        this.eventVersion = eventVersion;
        return this;
    }

    public PipesKinesisEvent setEventSourceARN(final String eventSourceARN) {
        this.eventSourceARN = eventSourceARN;
        return this;
    }

    public PipesKinesisEvent setAwsRegion(final String awsRegion) {
        this.awsRegion = awsRegion;
        return this;
    }
}
