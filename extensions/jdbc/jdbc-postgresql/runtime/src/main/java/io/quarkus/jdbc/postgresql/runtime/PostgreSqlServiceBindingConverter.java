package io.quarkus.jdbc.postgresql.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.kubernetes.service.binding.runtime.ServiceBinding;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConfigSource;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter;

public class PostgreSqlServiceBindingConverter implements ServiceBindingConverter {

    private static final Logger log = Logger.getLogger(ServiceBinding.class);

    @Override
    public Optional<ServiceBindingConfigSource> convert(List<ServiceBinding> serviceBindings) {
        Optional<ServiceBinding> matchingByType = ServiceBinding.singleMatchingByType("postgresql", serviceBindings);
        if (!matchingByType.isPresent()) {
            return Optional.empty();
        }

        Map<String, String> properties = new HashMap<>();
        ServiceBinding binding = matchingByType.get();

        String username = binding.getProperties().get("username");
        if (username != null) {
            properties.put("quarkus.datasource.username", username);
        } else {
            log.debug("Property 'username' was not found");
        }
        String password = binding.getProperties().get("password");
        if (password != null) {
            properties.put("quarkus.datasource.password", password);
        } else {
            log.debug("Property 'password' was not found");
        }
        String host = binding.getProperties().get("host");
        String port = binding.getProperties().get("port");
        String database = binding.getProperties().get("database");
        if ((host != null) && (database != null)) {
            String portPart = "";
            if (port != null) {
                portPart = ":" + port;
            }
            properties.put("quarkus.datasource.jdbc.url", String.format("jdbc:postgresql://%s%s/%s", host, portPart, database));
        } else {
            log.debug("One or more of 'host' or 'database' properties were not found");
        }
        return Optional.of(new ServiceBindingConfigSource("postgresql-k8s-service-binding-source", properties));
    }
}
