package io.quarkus.kubernetes.service.binding.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

/**
 * This utility collects the conversion of Service types that correspond to JDBC data-sources
 * into a single place as the code for all is the same.
 * This inevitably results in datasource related logic being places inside this (otherwise)
 * service binding agnostic module, but it's a small price to pay compared to the alternative
 * of copying and pasting the same code for all JDBC data-sources.
 */
public class JdbcDatasourceUtil {

    private static final Logger log = Logger.getLogger(JdbcDatasourceUtil.class);

    public static Optional<ServiceBindingConfigSource> convert(List<ServiceBinding> serviceBindings, String type) {
        return convert(serviceBindings, type, type);
    }

    public static Optional<ServiceBindingConfigSource> convert(List<ServiceBinding> serviceBindings, String bindingType,
            String urlType) {
        Optional<ServiceBinding> matchingByType = ServiceBinding.singleMatchingByType(bindingType, serviceBindings);
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
            properties.put("quarkus.datasource.jdbc.url",
                    String.format("jdbc:%s://%s%s/%s", urlType, host, portPart, database));
        } else {
            log.debug("One or more of 'host' or 'database' properties were not found");
        }
        return Optional.of(new ServiceBindingConfigSource(bindingType + "-k8s-service-binding-source", properties));
    }
}
