package io.quarkus.rest.client.reactive.runtime.devconsole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.impl.ClientProxies;
import org.jboss.resteasy.reactive.client.spi.ClientContext;
import org.jboss.resteasy.reactive.client.spi.ClientContextResolver;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.rest.client.reactive.runtime.RestClientRecorder;

@IfBuildProfile("dev")
@Unremovable
@Singleton
public class RestClientsContainer {

    public static final ClientContextResolver CLIENT_CONTEXT_RESOLVER = ClientContextResolver.getInstance();

    @Inject
    @RestClient
    Instance<Object> injectableClients;

    /**
     * Used in Dev UI
     *
     * @return info about exposed clients
     */
    public RestClientData getClientData() {
        ClientContext context = CLIENT_CONTEXT_RESOLVER.resolve(Thread.currentThread().getContextClassLoader());
        ClientProxies.ClientData clientData = context.getClientProxies().getClientData();

        List<RestClientInfo> restClients = new ArrayList<>();
        List<PossibleRestClientInfo> possibleRestClients = new ArrayList<>();

        for (Class<?> clientClass : clientData.clientClasses) {
            Instance<?> select = injectableClients.select(clientClass);
            String interfaceName = clientClass.getName();
            if (select.isResolvable()) {
                String configKey = RestClientRecorder.getConfigKeys().get(interfaceName);
                if (configKey == null) {
                    configKey = String.format("\"%s\"", interfaceName);
                }
                restClients.add(new RestClientInfo(interfaceName, true, configKey));
            } else {
                restClients.add(new RestClientInfo(interfaceName, false, null));
            }
        }
        restClients.sort(Comparator.comparing(info -> info.interfaceClass));
        for (Map.Entry<Class<?>, String> clientEntry : clientData.failures.entrySet()) {
            possibleRestClients.add(new PossibleRestClientInfo(clientEntry.getKey().getName(), clientEntry.getValue()));
        }
        possibleRestClients.sort(Comparator.comparing(info -> info.interfaceClass));
        return new RestClientData(restClients, possibleRestClients);
    }

    public static class RestClientData {
        public final List<RestClientInfo> clients;
        public final List<PossibleRestClientInfo> possibleClients;

        public RestClientData(List<RestClientInfo> clients, List<PossibleRestClientInfo> possibleClients) {
            this.clients = clients;
            this.possibleClients = possibleClients; // TODO: present this info
        }
    }

    public static class RestClientInfo {
        public final String interfaceClass;
        public final boolean isBean;
        public final String configKey;

        public RestClientInfo(String interfaceClass, boolean isBean, String configKey) {
            this.interfaceClass = interfaceClass;
            this.isBean = isBean;
            this.configKey = configKey == null ? "" : String.format("quarkus.rest.client.%s", configKey);
        }
    }

    public static class PossibleRestClientInfo {
        public final String interfaceClass;
        public final String failure;

        public PossibleRestClientInfo(String interfaceClass, String failure) {
            this.interfaceClass = interfaceClass;
            this.failure = failure;
        }
    }
}
