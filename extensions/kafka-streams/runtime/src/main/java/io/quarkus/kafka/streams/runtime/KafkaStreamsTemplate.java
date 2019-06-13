package io.quarkus.kafka.streams.runtime;

import org.rocksdb.RocksDB;

import io.quarkus.runtime.annotations.Template;

@Template
public class KafkaStreamsTemplate {

    public void loadRocksDb() {
        RocksDB.loadLibrary();
    }
}
