package io.quarkus.kafka.streams.runtime;

import java.util.Properties;

import org.rocksdb.RocksDB;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class KafkaStreamsRecorder {

    public void loadRocksDb() {
        RocksDB.loadLibrary();
    }

    public void configureRuntimeProperties(KafkaStreamsRuntimeConfig runtimeConfig) {
        Arc.container().instance(KafkaStreamsTopologyManager.class).get().setRuntimeConfig(runtimeConfig);
    }

    public BeanContainerListener configure(Properties properties) {
        return container -> {
            KafkaStreamsTopologyManager instance = container.instance(KafkaStreamsTopologyManager.class);
            instance.configure(properties);
        };
    }
}
