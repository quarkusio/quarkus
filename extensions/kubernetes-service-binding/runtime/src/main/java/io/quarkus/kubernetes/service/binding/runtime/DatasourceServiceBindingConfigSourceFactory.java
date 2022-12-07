package io.quarkus.kubernetes.service.binding.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jboss.logging.Logger;

public abstract class DatasourceServiceBindingConfigSourceFactory
        implements Function<ServiceBinding, ServiceBindingConfigSource> {

    private static final Logger log = Logger.getLogger(DatasourceServiceBindingConfigSourceFactory.class);

    private final String configSourceNamePrefix;
    private final String urlPropertyName;
    private final String urlFormat;
    protected ServiceBinding serviceBinding;

    private DatasourceServiceBindingConfigSourceFactory(String configSourceNamePrefix, String urlPropertyName,
            String urlFormat) {
        this.configSourceNamePrefix = configSourceNamePrefix;
        this.urlPropertyName = urlPropertyName;
        this.urlFormat = urlFormat;
    }

    @Override
    public final ServiceBindingConfigSource apply(ServiceBinding serviceBinding) {
        this.serviceBinding = serviceBinding;
        String name = configSourceNamePrefix + "-" + serviceBinding.getType() + "-k8s-service-binding-source";
        return new ServiceBindingConfigSource(name, getServiceBindingProperties());
    }

    private Map<String, String> getServiceBindingProperties() {
        Map<String, String> properties = new HashMap<>();
        Map<String, String> bindingProperties = serviceBinding.getProperties();

        String username = bindingProperties.get("username");
        if (username != null) {
            properties.put("quarkus.datasource.username", username);
        } else {
            log.debugf("Property 'username' was not found for datasource of type %s", serviceBinding.getType());
        }
        String password = bindingProperties.get("password");
        if (password != null) {
            properties.put("quarkus.datasource.password", password);
        } else {
            log.debugf("Property 'password' was not found for datasource of type %s", serviceBinding.getType());
        }

        String jdbcURL = bindingProperties.get("jdbc-url");
        if (jdbcURL != null) {
            properties.put(urlPropertyName, jdbcURL);
            return properties;
        }
        String uri = bindingProperties.get("uri");
        if (uri != null) {
            properties.put(urlPropertyName, uri);
            return properties;
        }

        String host = bindingProperties.get("host");
        String port = bindingProperties.get("port");
        String database = bindingProperties.get("database");
        if ((host != null) && (database != null)) {
            String portPart = "";
            if (port != null) {
                portPart = ":" + port;
            }
            properties.put(urlPropertyName, formatUrl(urlFormat, serviceBinding.getType(), host, database, portPart));
        } else {
            log.warnf("One or more of 'host' or 'database' properties were not found for datasource of type %s",
                    serviceBinding.getType());
        }

        return properties;
    }

    protected String formatUrl(String urlFormat, String type, String host, String database, String portPart) {
        return String.format(urlFormat, type, host, portPart, database);
    }

    public static class Jdbc extends DatasourceServiceBindingConfigSourceFactory {
        public Jdbc() {
            super("jdbc", "quarkus.datasource.jdbc.url", "jdbc:%s://%s%s/%s");
        }

        public Jdbc(String urlFormat) {
            super("jdbc", "quarkus.datasource.jdbc.url", urlFormat);
        }
    }

    public static class Reactive extends DatasourceServiceBindingConfigSourceFactory {
        public Reactive() {
            super("reactive", "quarkus.datasource.reactive.url", "%s://%s%s/%s");
        }

        public Reactive(String urlFormat) {
            super("reactive", "quarkus.datasource.reactive.url", urlFormat);
        }
    }
}
