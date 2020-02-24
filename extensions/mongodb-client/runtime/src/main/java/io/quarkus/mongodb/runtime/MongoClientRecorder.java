package io.quarkus.mongodb.runtime;

import java.util.List;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.util.AnnotationLiteral;

import com.mongodb.client.MongoClient;
import com.mongodb.event.ConnectionPoolListener;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MongoClientRecorder {

    public static final String DEFAULT_MONGOCLIENT_NAME = "<default>";
    public static final String REACTIVE_CLIENT_NAME_SUFFIX = "reactive";

    public BeanContainerListener addMongoClient(
            Class<? extends AbstractMongoClientProducer> mongoClientProducerClass,
            boolean disableSslSupport) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer beanContainer) {
                AbstractMongoClientProducer producer = beanContainer.instance(mongoClientProducerClass);
                if (disableSslSupport) {
                    producer.disableSslSupport();
                }
            }
        };
    }

    public void configureRuntimeProperties(List<String> codecs, List<String> bsonDiscriminators, MongodbConfig config,
            List<ConnectionPoolListener> connectionPoolListeners) {
        // TODO @dmlloyd
        // Same here, the map is entirely empty (obviously, I didn't expect the values
        // that were not properly injected but at least the config objects present in
        // the map)
        // The elements from the default mongoClient are there
        AbstractMongoClientProducer producer = Arc.container().instance(AbstractMongoClientProducer.class).get();
        producer.setCodecs(codecs);
        producer.setBsonDiscriminators(bsonDiscriminators);
        producer.setConfig(config);
        producer.setConnectionPoolListeners(connectionPoolListeners);
    }

    public RuntimeValue<MongoClient> getClient(String name) {
        return new RuntimeValue<>(Arc.container().instance(MongoClient.class, literal(name)).get());
    }

    public RuntimeValue<ReactiveMongoClient> getReactiveClient(String name) {
        return new RuntimeValue<>(
                Arc.container().instance(ReactiveMongoClient.class, literal(name + REACTIVE_CLIENT_NAME_SUFFIX)).get());
    }

    @SuppressWarnings("rawtypes")
    private AnnotationLiteral literal(String name) {
        if (name.startsWith(DEFAULT_MONGOCLIENT_NAME)) {
            return Default.Literal.INSTANCE;
        }
        return NamedLiteral.of(name);
    }
}
