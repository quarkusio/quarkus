package io.quarkus.test.hazelcast;

import java.util.Collections;
import java.util.Map;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class HazelcastServerTestResource implements QuarkusTestResourceLifecycleManager {

    private volatile HazelcastInstance member;

    @Override
    public Map<String, String> start() {
        member = Hazelcast.newHazelcastInstance();
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (member != null) {
            member.shutdown();
        }
    }
}
