package io.quarkus.hazelcast.client.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HazelcastClientBytecodeRecorder {

    public void configureRuntimeProperties(HazelcastClientConfig config) {
        HazelcastClientProducer producer = Arc.container().instance(HazelcastClientProducer.class).get();
        producer.injectConfig(config);
    }
}
