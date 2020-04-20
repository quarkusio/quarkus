package io.quarkus.dynamodb.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Recorder
public class DynamodbRecorder {

    public RuntimeValue<DynamoDbClient> createClient(
            BeanContainer beanContainer, ShutdownContext shutdown) {

        DynamodbClientProducer producer = beanContainer.instance(DynamodbClientProducer.class);
        shutdown.addShutdownTask(producer::destroy);
        return new RuntimeValue<>(producer.client());
    }

    public RuntimeValue<DynamoDbAsyncClient> createAsyncClient(
            BeanContainer beanContainer, ShutdownContext shutdown) {

        DynamodbClientProducer producer = beanContainer.instance(DynamodbClientProducer.class);
        shutdown.addShutdownTask(producer::destroy);
        return new RuntimeValue<>(producer.asyncClient());
    }
}
