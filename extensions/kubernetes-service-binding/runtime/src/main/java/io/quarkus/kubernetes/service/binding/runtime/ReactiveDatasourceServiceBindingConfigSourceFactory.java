package io.quarkus.kubernetes.service.binding.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jboss.logging.Logger;

public class ReactiveDatasourceServiceBindingConfigSourceFactory
        implements Function<ServiceBinding, ServiceBindingConfigSource> {

    private static final Logger log = Logger.getLogger(ReactiveDatasourceServiceBindingConfigSourceFactory.class);

    private final String type;

    public ReactiveDatasourceServiceBindingConfigSourceFactory(String type) {
        this.type = type;
    }

    @Override
    public ServiceBindingConfigSource apply(ServiceBinding serviceBinding) {
        return new ServiceBindingConfigSource("reactive-" + type + "-k8s-service-binding-source",
                getServiceBindingProperties(serviceBinding));
    }

    private Map<String, String> getServiceBindingProperties(ServiceBinding binding) {
        Map<String, String> properties = new HashMap<>();
        Map<String, String> bindingProperties = binding.getProperties();

        String username = bindingProperties.get("username");
        if (username != null) {
            properties.put("quarkus.datasource.username", username);
        } else {
            log.debugf("Property 'username' was not found for datasource of type %s", type);
        }
        String password = bindingProperties.get("password");
        if (password != null) {
            properties.put("quarkus.datasource.password", password);
        } else {
            log.debugf("Property 'password' was not found for datasource of type %s", type);
        }

        String host = bindingProperties.get("host");
        String port = bindingProperties.get("port");
        String database = bindingProperties.get("database");
        if ((host != null) && (database != null)) {
            String portPart = "";
            if (port != null) {
                portPart = ":" + port;
            }
            properties.put(urlPropertyName(), formatUrl(host, database, portPart));
        } else {
            log.debugf("One or more of 'host' or 'database' properties were not found for datasource of type %s", type);
        }

        return properties;
    }

    protected String urlPropertyName() {
        return "quarkus.datasource.reactive.url";
    }

    protected String formatUrl(String host, String database, String portPart) {
        return String.format("%s://%s%s/%s", type, host, portPart, database);
    }
}
