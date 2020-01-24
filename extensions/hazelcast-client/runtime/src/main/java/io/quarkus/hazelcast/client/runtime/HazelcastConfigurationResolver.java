package io.quarkus.hazelcast.client.runtime;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.impl.clientside.FailoverClientConfigSupport;

class HazelcastConfigurationResolver {
    private final HazelcastConfigurationParser parser = new HazelcastConfigurationParser();

    ClientConfig resolveClientConfig(HazelcastClientConfig properties) {
        ClientConfig clientConfig = FailoverClientConfigSupport.resolveClientConfig(null);
        return parser.fromApplicationProperties(properties, clientConfig);
    }
}
