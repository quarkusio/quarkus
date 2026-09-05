package io.quarkus.rest.client.reactive.runtime.produi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.impl.ClientProxies;
import org.jboss.resteasy.reactive.client.spi.ClientContext;
import org.jboss.resteasy.reactive.client.spi.ClientContextResolver;

import io.quarkus.rest.client.reactive.runtime.RestClientRecorder;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.restclient.config.RestClientsConfig.RestClientConfig;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.config.SmallRyeConfig;

/**
 * Read-only Prod UI view of the registered REST clients. For each client it
 * exposes the client interface, its config key, whether it is an injectable CDI
 * bean and its configured base URL, derived from the always-present runtime
 * {@link ClientContext} and {@link RestClientsConfig}. The Dev UI is not reused:
 * its component pulls in dev-only web components and its backing container is
 * {@code @IfBuildProfile("dev")}. Any credentials embedded in a base URL
 * (userinfo) are stripped before being returned, and no request can be invoked.
 */
@ApplicationScoped
public class RestClientReactiveProdUIService {

    private static final ClientContextResolver CLIENT_CONTEXT_RESOLVER = ClientContextResolver.getInstance();

    @Inject
    @RestClient
    Instance<Object> injectableClients;

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only list of the configured REST clients with their base URLs (credentials stripped)")
    public List<ClientInfo> getClients() {
        ClientContext context = CLIENT_CONTEXT_RESOLVER.resolve(Thread.currentThread().getContextClassLoader());
        ClientProxies.ClientData clientData = context.getClientProxies().getClientData();

        RestClientsConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class)
                .getConfigMapping(RestClientsConfig.class);
        Map<String, String> configKeys = RestClientRecorder.getConfigKeys();
        if (configKeys == null) {
            configKeys = Map.of();
        }

        List<ClientInfo> clients = new ArrayList<>();
        for (Class<?> clientClass : clientData.clientClasses) {
            String interfaceName = clientClass.getName();
            boolean isBean = injectableClients.select(clientClass).isResolvable();
            String configKey = configKeys.get(interfaceName);
            String baseUrl = sanitize(baseUrlOf(config, interfaceName));
            clients.add(new ClientInfo(interfaceName, configKey, isBean, baseUrl));
        }
        clients.sort(Comparator.comparing(ClientInfo::clientInterface));
        return clients;
    }

    private String baseUrlOf(RestClientsConfig config, String interfaceName) {
        RestClientConfig clientConfig = config.clients().get(interfaceName);
        if (clientConfig == null) {
            return null;
        }
        return clientConfig.overrideUri()
                .or(clientConfig::uri)
                .or(clientConfig::url)
                .orElse(null);
    }

    /**
     * Removes any credentials embedded in a base URL (the {@code user:password@}
     * userinfo component) so secrets are never exposed in the Prod UI.
     */
    private String sanitize(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = new URI(url);
            if (uri.getUserInfo() != null) {
                return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                        uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
            }
            return url;
        } catch (URISyntaxException e) {
            // If the URL cannot be parsed, still strip any embedded userinfo defensively
            return url.replaceFirst("://[^/@]*@", "://");
        }
    }

    public record ClientInfo(String clientInterface, String configKey, boolean isBean, String baseUrl) {
    }
}
