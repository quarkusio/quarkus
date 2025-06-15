package io.quarkus.infinispan.client.runtime;

import static java.lang.String.format;
import static org.apache.sshd.common.util.GenericUtils.isBlank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.kubernetes.service.binding.runtime.ServiceBinding;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConfigSource;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter;

/**
 * ServiceBindingConverter for Infinispan to support SBO (Service Binding Operator) in Quarkus. <br>
 * <br>
 * Following individual properties are supported to make the connection:
 * <ul>
 * <li>uri</li>
 * <li>hosts(<i>host information can be provided in this property separated by : sign</i>, e.g. localhost:11222</li>)
 * <li>useauth</li>
 * <li>username</li>
 * <li>password</li>
 * </ul>
 * <i>if uri is provided, all the other properties are ignored</i> <i>if uri is not provided, hosts is mandatory and all
 * other properties are optional</i> <br>
 * <br>
 * The Quarkus properties set by this class are:
 * <ul>
 * <li>quarkus.infinispan-client.uri (<i>if uri is provided</i>)</li>
 * <li>quarkus.infinispan-client.hosts (<i>if hosts is provided and uri is not provided</i>)</li>
 * <li>quarkus.infinispan-client.use-auth (<i>if useauth is provided and uri is not provided</i>)</li>
 * <li>quarkus.infinispan-client.username (<i>if username is provided and uri is not provided</i>)</li>
 * <li>quarkus.infinispan-client.password (<i>if password is provided and uri is not provided</i>)</li>
 * </ul>
 */
public class InfinispanServiceBindingConverter implements ServiceBindingConverter {

    private static final Logger LOGGER = Logger.getLogger(InfinispanServiceBindingConverter.class);
    private static final String BINDING_TYPE = "infinispan";
    public static final String BINDING_CONFIG_SOURCE_NAME = BINDING_TYPE + "-k8s-service-binding-source";

    // Connection properties
    public static final String INFINISPAN_URI = "uri";
    public static final String INFINISPAN_HOSTS = "hosts";
    public static final String INFINISPAN_USE_AUTH = "useauth";
    public static final String INFINISPAN_USERNAME = "username";
    public static final String INFINISPAN_PASSWORD = "password";

    // Infinispan Quarkus properties
    public static final String INFINISPAN_CLIENT_URI = "quarkus.infinispan-client.uri";
    public static final String INFINISPAN_CLIENT_HOSTS = "quarkus.infinispan-client.hosts";
    public static final String INFINISPAN_CLIENT_USE_AUTH = "quarkus.infinispan-client.use-auth";
    public static final String INFINISPAN_CLIENT_AUTH_USERNAME = "quarkus.infinispan-client.username";
    public static final String INFINISPAN_CLIENT_AUTH_PASSWORD = "quarkus.infinispan-client.password";

    @Override
    public Optional<ServiceBindingConfigSource> convert(List<ServiceBinding> serviceBindings) {
        Optional<ServiceBinding> matchingByType = ServiceBinding.singleMatchingByType(BINDING_TYPE, serviceBindings);
        if (matchingByType.isEmpty()) {
            return Optional.empty();
        }

        ServiceBinding binding = matchingByType.get();
        Map<String, String> properties = new HashMap<>();

        setUri(binding, properties);
        setConnection(binding, properties);
        setUseAuth(binding, properties);
        setUsername(binding, properties);
        setPassword(binding, properties);

        return Optional.of(new ServiceBindingConfigSource(BINDING_CONFIG_SOURCE_NAME, properties));
    }

    private void setUri(ServiceBinding binding, Map<String, String> properties) {
        properties.put(INFINISPAN_CLIENT_URI, getInfinispanProperty(binding, INFINISPAN_URI));
    }

    private void setConnection(ServiceBinding binding, Map<String, String> properties) {
        properties.put(INFINISPAN_CLIENT_HOSTS, getInfinispanProperty(binding, INFINISPAN_HOSTS));
    }

    private void setUseAuth(ServiceBinding binding, Map<String, String> properties) {
        properties.put(INFINISPAN_CLIENT_USE_AUTH, getInfinispanProperty(binding, INFINISPAN_USE_AUTH));
    }

    private void setUsername(ServiceBinding binding, Map<String, String> properties) {
        properties.put(INFINISPAN_CLIENT_AUTH_USERNAME, getInfinispanProperty(binding, INFINISPAN_USERNAME));
    }

    private void setPassword(ServiceBinding binding, Map<String, String> properties) {
        properties.put(INFINISPAN_CLIENT_AUTH_PASSWORD, getInfinispanProperty(binding, INFINISPAN_PASSWORD));
    }

    private String getInfinispanProperty(ServiceBinding binding, String infinispanPropertyKey) {
        String infinispanPropertyValue = binding.getProperties().get(infinispanPropertyKey);
        if (isBlank(infinispanPropertyValue)) {
            LOGGER.debug(format("Property '%s' not found", infinispanPropertyKey));
        }

        return infinispanPropertyValue;
    }

}
