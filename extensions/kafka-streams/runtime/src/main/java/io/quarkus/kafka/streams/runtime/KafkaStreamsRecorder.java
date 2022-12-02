package io.quarkus.kafka.streams.runtime;

import java.util.Properties;
import java.util.function.Supplier;

import org.rocksdb.RocksDB;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class KafkaStreamsRecorder {

    public void loadRocksDb() {
        RocksDB.loadLibrary();
    }

    public Supplier<KafkaStreamsSupport> kafkaStreamsSupportSupplier(Properties properties) {
        return new Supplier<KafkaStreamsSupport>() {
            @Override
            public KafkaStreamsSupport get() {
                return new KafkaStreamsSupport(properties);
            }
        };
    }
}
