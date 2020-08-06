package io.quarkus.azure.app.config.client.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

public class AzureAppConfigServerClientConfigSourceProvider implements ConfigSourceProvider {

    private static final Logger log = Logger.getLogger(AzureAppConfigServerClientConfigSourceProvider.class);

    private final AzureAppConfigClientConfig azureAppConfigClientConfig;

    private final AzureAppConfigClientGateway azureAppConfigClientGateway;

    public AzureAppConfigServerClientConfigSourceProvider(AzureAppConfigClientConfig appCfgClientConfig) {
        this.azureAppConfigClientConfig = appCfgClientConfig;
        this.azureAppConfigClientGateway = new DefaultAzureAppConfigClientGateway(appCfgClientConfig);
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        try {
            final Response response = azureAppConfigClientGateway.exchange();
            final List<Response.Item> items = response.getItems();

            Map<String, String> props = new HashMap<>();
            for (Response.Item i : items) {
                props.put(i.getKey(), i.getValue());
            }
            InMemoryConfigSource cs = new InMemoryConfigSource(450, "azure-app-configsource", props);
            return Collections.singletonList(cs);
        } catch (Exception e) {
            final String errorMessage = "Unable to obtain configuration from Azure App Config Server at "
                    + azureAppConfigClientConfig.url;
            if (azureAppConfigClientConfig.failFast) {
                throw new RuntimeException(errorMessage, e);
            } else {
                log.error(errorMessage, e);
                return Collections.emptyList();
            }
        }
    }

    private static final class InMemoryConfigSource implements ConfigSource {

        private final Map<String, String> values = new HashMap<>();
        private final int ordinal;
        private final String name;

        private InMemoryConfigSource(int ordinal, String name, Map<String, String> source) {
            this.ordinal = ordinal;
            this.name = name;
            this.values.putAll(source);
        }

        @Override
        public Map<String, String> getProperties() {
            return values;
        }

        @Override
        public Set<String> getPropertyNames() {
            return values.keySet();
        }

        @Override
        public int getOrdinal() {
            return ordinal;
        }

        @Override
        public String getValue(String propertyName) {
            return values.get(propertyName);
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
