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

    public static final String QUARKUS_DATASOURCE_JDBC_URL = "quarkus.datasource.jdbc.url";
    private static final String QUARKUS_DATASOURCE_USERNAME = "quarkus.datasource.username";
    private static final String QUARKUS_DATASOURCE_PASSWORD = "quarkus.datasource.password";

    public static Optional<ServiceBindingConfigSource> convert(List<ServiceBinding> serviceBindings, String type) {
        return convert(serviceBindings, type, type);
    }

    public static Optional<ServiceBindingConfigSource> convert(List<ServiceBinding> serviceBindings, String bindingType,
            String urlType) {
        Optional<ServiceBinding> matchingByType = ServiceBinding.singleMatchingByType(bindingType, serviceBindings);
        if (!matchingByType.isPresent()) {
            return Optional.empty();
        }

        ServiceBinding binding = matchingByType.get();

        return Optional.of(new ServiceBindingConfigSource(bindingType + "-k8s-service-binding-source",
                getServiceBindingProperties(binding, urlType)));
    }

    public static Map<String, String> getServiceBindingProperties(ServiceBinding binding, String urlType) {
        Map<String, String> properties = new HashMap<>();

        String username = binding.getProperties().get("username");
        if (username != null) {
            properties.put(QUARKUS_DATASOURCE_USERNAME, username);
        } else {
            log.debug("Property 'username' was not found");
        }
        String password = binding.getProperties().get("password");
        if (password != null) {
            properties.put(QUARKUS_DATASOURCE_PASSWORD, password);
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
            properties.put(QUARKUS_DATASOURCE_JDBC_URL,
                    String.format("jdbc:%s://%s%s/%s", urlType, host, portPart, database));
        } else {
            log.debug("One or more of 'host' or 'database' properties were not found");
        }

        return properties;
    }
}
