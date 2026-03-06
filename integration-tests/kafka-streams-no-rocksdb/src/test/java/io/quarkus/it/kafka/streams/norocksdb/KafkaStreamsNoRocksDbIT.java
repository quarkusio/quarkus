package io.quarkus.it.kafka.streams.norocksdb;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Native integration test for Kafka Streams with RocksDB disabled.
 * <p>
 * This runs the same tests as {@link KafkaStreamsNoRocksDbTest} but against the
 * native executable, which was built with {@code quarkus.kafka-streams.rocksdb.enabled=false}.
 * This verifies that the native image works without RocksDB JNI registration,
 * native lib loading, and the GraalVM RocksDB feature.
 */
@QuarkusIntegrationTest
public class KafkaStreamsNoRocksDbIT extends KafkaStreamsNoRocksDbTest {

}
