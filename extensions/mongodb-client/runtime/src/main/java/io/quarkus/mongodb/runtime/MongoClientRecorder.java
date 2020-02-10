package io.quarkus.mongodb.runtime;

import java.util.List;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.mongodb.ReactiveMongoClient;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MongoClientRecorder {

    public static final String DEFAULT_MONGOCLIENT_NAME = "<default>";

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

    public void configureRuntimeProperties(List<String> codecs, List<String> bsonDiscriminators, MongodbConfig config) {
        // TODO @dmlloyd
        // Same here, the map is entirely empty (obviously, I didn't expect the values
        // that were not properly injected but at least the config objects present in
        // the map)
        // The elements from the default mongoClient are there
        AbstractMongoClientProducer producer = Arc.container().instance(AbstractMongoClientProducer.class).get();
        producer.setCodecs(codecs);
        producer.setBsonDiscriminators(bsonDiscriminators);
        producer.setConfig(config);
    }

    public RuntimeValue<MongoClient> getClient(String name) {
        AbstractMongoClientProducer producer = Arc.container().instance(AbstractMongoClientProducer.class).get();
        return new RuntimeValue<>(producer.getClient(name));
    }

    public RuntimeValue<ReactiveMongoClient> getReactiveClient(String name) {
        AbstractMongoClientProducer producer = Arc.container().instance(AbstractMongoClientProducer.class).get();
        return new RuntimeValue<>(producer.getReactiveClient(name));
    }

}
