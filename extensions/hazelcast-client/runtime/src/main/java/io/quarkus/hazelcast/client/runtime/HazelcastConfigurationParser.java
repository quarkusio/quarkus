package io.quarkus.hazelcast.client.runtime;

import java.net.InetSocketAddress;

import com.hazelcast.client.config.ClientConfig;

class HazelcastConfigurationParser {

    ClientConfig fromApplicationProperties(HazelcastClientConfig config, ClientConfig clientConfig) {
        setClusterAddress(clientConfig, config);
        setLabels(clientConfig, config);

        setOutboundPorts(clientConfig, config);
        setOutboundPortDefinitions(clientConfig, config);

        setConnectionTimeout(clientConfig, config);

        return clientConfig;
    }

    private void setClusterAddress(ClientConfig clientConfig, HazelcastClientConfig config) {
        if (config.clusterMembers.isPresent()) {
            for (InetSocketAddress clusterMember : config.clusterMembers.get()) {
                clientConfig.getNetworkConfig().addAddress(clusterMember.toString());
            }
        }
    }

    private void setLabels(ClientConfig clientConfig, HazelcastClientConfig config) {
        if (config.labels.isPresent()) {
            for (String label : config.labels.get()) {
                clientConfig.addLabel(label);
            }
        }
    }

    private void setConnectionTimeout(ClientConfig clientConfig, HazelcastClientConfig config) {
        if (config.connectionTimeout.isPresent()) {
            int timeout = config.connectionTimeout.getAsInt();
            clientConfig.getNetworkConfig().setConnectionTimeout(timeout);
        }
    }

    private void setOutboundPortDefinitions(ClientConfig clientConfig, HazelcastClientConfig config) {
        if (config.outboundPortDefinitions.isPresent()) {
            for (String outboundPortDefinition : config.outboundPortDefinitions.get()) {
                clientConfig.getNetworkConfig().addOutboundPortDefinition(outboundPortDefinition);
            }
        }
    }

    private void setOutboundPorts(ClientConfig clientConfig, HazelcastClientConfig config) {
        if (config.outboundPorts.isPresent()) {
            for (Integer outboundPort : config.outboundPorts.get()) {
                clientConfig.getNetworkConfig().addOutboundPort(outboundPort);
            }
        }
    }
}
