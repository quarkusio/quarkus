package io.quarkus.kafka.client.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.kubernetes.service.binding.runtime.ServiceBinding;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConfigSource;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter;

public class KafkaBindingConverter implements ServiceBindingConverter {

    @Override
    public Optional<ServiceBindingConfigSource> convert(List<ServiceBinding> serviceBindings) {
        Optional<ServiceBinding> matchingByType = ServiceBinding.singleMatchingByType("kafka", serviceBindings);
        if (!matchingByType.isPresent()) {
            return Optional.empty();
        }

        Map<String, String> properties = new HashMap<>();
        ServiceBinding binding = matchingByType.get();

        String bootstrapServers = binding.getProperties().get("bootstrapServers");
        if (bootstrapServers == null) {
            bootstrapServers = binding.getProperties().get("bootstrap-servers");
        }
        if (bootstrapServers != null) {
            properties.put("kafka.bootstrap.servers", bootstrapServers);
        }

        String securityProtocol = binding.getProperties().get("securityProtocol");
        if (securityProtocol != null) {
            properties.put("kafka.security.protocol", securityProtocol);
        }

        String saslMechanism = binding.getProperties().get("saslMechanism");
        if (saslMechanism != null) {
            properties.put("kafka.sasl.mechanism", saslMechanism);
        }
        String user = binding.getProperties().get("user");
        String password = binding.getProperties().get("password");
        if ("PLAIN".equals(saslMechanism) && (user != null) && (password != null)) {
            properties.put("kafka.sasl.jaas.config",
                    String.format(
                            "org.apache.kafka.common.security.plain.PlainLoginModule required username='%s' password='%s';",
                            user, password));
        }

        return Optional.of(new ServiceBindingConfigSource("kafka-k8s-service-binding-source", properties));
    }
}
