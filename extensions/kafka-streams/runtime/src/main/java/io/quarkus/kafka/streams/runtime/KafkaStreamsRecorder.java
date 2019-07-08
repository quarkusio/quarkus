package io.quarkus.kafka.streams.runtime;

import org.rocksdb.RocksDB;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class KafkaStreamsRecorder {

    public void loadRocksDb() {
        RocksDB.loadLibrary();
    }
}
