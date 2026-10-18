package io.quarkus.kafka.streams.runtime;

import java.util.Map;
import java.util.Properties;

import org.rocksdb.RocksDB;

public class KafkaStreamsRecorder {

    public static void loadRocksDb() {
        RocksDB.loadLibrary();
    }

    public static KafkaStreamsSupport kafkaStreamsSupport(Map<String, String> properties) {
        Properties kafkaStreamsProperties = new Properties();
        kafkaStreamsProperties.putAll(properties);
        return new KafkaStreamsSupport(kafkaStreamsProperties);
    }
}
