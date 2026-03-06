package io.quarkus.it.kafka.streams;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * Runs the full Kafka Streams test suite with {@code quarkus.kafka-streams.rocksdb.enabled=false}.
 * <p>
 * This verifies that disabling RocksDB native support at build time does not
 * break Kafka Streams when using in-memory state stores (which the test topology does).
 * In JVM mode, RocksDB JARs are still on the classpath but the build-time registration
 * steps (JNI, native lib loading, GraalVM feature) are skipped.
 */
@QuarkusTestResource(KafkaSSLTestResource.class)
@TestProfile(KafkaStreamsRocksDbDisabledTest.RocksDbDisabledProfile.class)
@QuarkusTest
public class KafkaStreamsRocksDbDisabledTest extends KafkaStreamsTest {

    public static class RocksDbDisabledProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> conf = new HashMap<>();
            conf.put("quarkus.kafka-streams.rocksdb.enabled", "false");
            return conf;
        }
    }
}
