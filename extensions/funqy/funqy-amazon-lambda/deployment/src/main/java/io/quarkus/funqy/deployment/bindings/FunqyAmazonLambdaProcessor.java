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

public class FunqyAmazonLambdaProcessor {

    @BuildStep(onlyIf = NativeBuild.class)
    public void process(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
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
                // DynamoDB
                DynamodbEvent.class.getName(),
                DynamodbEvent.DynamodbStreamRecord.class.getName()
            ).constructors().methods().fields().build()
        );
    }
}
