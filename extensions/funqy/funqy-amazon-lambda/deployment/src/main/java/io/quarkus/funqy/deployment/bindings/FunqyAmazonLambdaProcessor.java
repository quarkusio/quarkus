package io.quarkus.funqy.deployment.bindings;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.events.models.kinesis.Record;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.funqy.lambda.model.cloudevents.CloudEventDataV1;
import io.quarkus.funqy.lambda.model.cloudevents.CloudEventV1;
import io.quarkus.funqy.lambda.model.kinesis.PipesKinesisEvent;
import io.quarkus.funqy.lambda.model.pipes.BatchItemFailures;
import io.quarkus.funqy.lambda.model.pipes.Response;

public class FunqyAmazonLambdaProcessor {

    @BuildStep(onlyIf = NativeBuild.class)
    public void process(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                // io CloudEvents
                CloudEventV1.class.getName(),
                CloudEventDataV1.class.getName(),
                // SQS
                SQSEvent.class.getName(),
                SQSEvent.SQSMessage.class.getName(),
                SQSEvent.MessageAttribute.class.getName(),
                SQSBatchResponse.class.getName(),
                SQSBatchResponse.BatchItemFailure.class.getName(),
                // SNS
                SNSEvent.class.getName(),
                SNSEvent.SNSRecord.class.getName(),
                SNSEvent.SNS.class.getName(),
                // Kinesis
                KinesisEvent.class.getName(),
                KinesisEvent.KinesisEventRecord.class.getName(),
                Record.class.getName(),
                StreamsEventResponse.class.getName(),
                StreamsEventResponse.BatchItemFailure.class.getName(),
                PipesKinesisEvent.class.getName(),
                // DynamoDB
                DynamodbEvent.class.getName(),
                DynamodbEvent.DynamodbStreamRecord.class.getName(),
                // Pipes
                Response.class.getName(),
                BatchItemFailures.class.getName()).constructors().methods().fields().build());
    }
}
