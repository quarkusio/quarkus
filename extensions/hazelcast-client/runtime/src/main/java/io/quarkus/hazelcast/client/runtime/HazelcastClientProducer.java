package io.quarkus.hazelcast.client.runtime;

import static com.hazelcast.client.HazelcastClient.newHazelcastClient;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;

import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class HazelcastClientProducer {
    HazelcastClientConfig config;

    @Produces
    @Singleton
    @DefaultBean
    public HazelcastInstance hazelcastClientInstance() {
        return newHazelcastClient(new HazelcastConfigurationResolver()
                .resolveClientConfig(config));
    }

    @PreDestroy
    public void destroy() {
        HazelcastClient.shutdownAll();
    }

    public void injectConfig(HazelcastClientConfig config) {
        this.config = config;
    }
}
