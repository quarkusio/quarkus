package io.quarkus.apicurio.registry.binding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.kubernetes.service.binding.runtime.ServiceBinding;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConfigSource;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter;

public class ServiceRegistryBindingConverter implements ServiceBindingConverter {

    private static Logger LOG = Logger.getLogger(ServiceRegistryBindingConverter.class.getName());

    private static final String INCOMING_PREFIX = "mp.messaging.incoming.";
    private static final String OUTGOING_PREFIX = "mp.messaging.outgoing.";

    @Override
    public Optional<ServiceBindingConfigSource> convert(List<ServiceBinding> serviceBindings) {
        var matchingByType = ServiceBinding.singleMatchingByType("serviceregistry", serviceBindings);
        Config config = ConfigProvider.getConfig();
        if (matchingByType.isEmpty()) {
            return Optional.empty();
        }

        var binding = matchingByType.get();

        List<String> channels = extractChannels(config);

        Map<String, String> properties = new HashMap<>();

        String registryUrl = binding.getProperties().get("registryUrl");
        if (registryUrl == null) {
            registryUrl = binding.getProperties().get("registryurl");
        }
        if (registryUrl != null) {
            properties.put("kafka.apicurio.registry.url", registryUrl);
        }

        for (String channel : channels) {

            String prefix = channel;

            String oauthTokenUrl = binding.getProperties().get("oauthTokenUrl");
            if (oauthTokenUrl == null) {
                oauthTokenUrl = binding.getProperties().get("oauthtokenurl");
            }
            if (oauthTokenUrl != null) {
                properties.put(prefix + "apicurio.auth.service.token.endpoint", oauthTokenUrl);
            }

            String clientId = binding.getProperties().get("clientId");
            if (clientId == null) {
                clientId = binding.getProperties().get("clientid");
            }
            if (clientId != null) {
                properties.put(prefix + "apicurio.auth.client.id", clientId);
            }

            String clientSecret = binding.getProperties().get("clientSecret");
            if (clientSecret == null) {
                clientSecret = binding.getProperties().get("clientsecret");
            }
            if (clientSecret != null) {
                properties.put(prefix + "apicurio.auth.client.secret", clientSecret);
            }
            if (registryUrl != null) {
                properties.put(prefix + "apicurio.registry.url", registryUrl);
            }
        }

        return Optional.of(new ServiceBindingConfigSource("serviceregistry-k8s-service-binding-source", properties));
    }

    private List<String> extractChannels(Config configIn) {

        var list = new ArrayList<String>();

        for (String propertyName : configIn.getPropertyNames()) {
            if (propertyName.startsWith(INCOMING_PREFIX)) {
                var channelAndProp = StringUtils.substringAfter(propertyName, INCOMING_PREFIX);
                // remove property
                var channelName = StringUtils.substringBefore(channelAndProp, ".");
                if (!StringUtils.isBlank(channelName))
                    list.add(INCOMING_PREFIX + channelName + ".");
            } else if (propertyName.startsWith(OUTGOING_PREFIX)) {
                var channelAndProp = StringUtils.substringAfter(propertyName, OUTGOING_PREFIX);
                // remove property
                var channelName = StringUtils.substringBefore(channelAndProp, ".");
                if (!StringUtils.isBlank(channelName))
                    list.add(OUTGOING_PREFIX + channelName + ".");
            }
        }
        return list;
    }
}
