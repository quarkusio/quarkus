package io.quarkus.rest.client.reactive.deployment.devservices;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.UncheckedException;
import org.jboss.logging.Logger;

import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.ApplicationInstanceIdBuildItem;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesRegistry;
import io.quarkus.rest.client.reactive.spi.DevServicesRestClientProxyProvider;
import io.quarkus.rest.client.reactive.spi.RestClientHttpProxyBuildItem;
import io.quarkus.restclient.config.RegisteredRestClient;
import io.quarkus.restclient.config.RestClientsBuildTimeConfig.RestClientBuildConfig;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.restclient.config.RestClientsConfig.RestClientConfig;
import io.quarkus.restclient.config.deployment.RestClientsBuildTimeConfigBuildItem;
import io.quarkus.runtime.Address;
import io.quarkus.runtime.AddressResolver;

@BuildSteps(onlyIf = IsDevServicesSupportedByLaunchMode.class)
public class DevServicesRestClientHttpProxyProcessor {

    private static final Logger log = Logger.getLogger(DevServicesRestClientHttpProxyProcessor.class);

    // the following fields are needed for state management as providers can come and go; the proxies themselves are
    // Dev Services, closed by the Dev Services registry
    private static final AtomicReference<Set<DevServicesRestClientProxyProvider>> runningProviders = new AtomicReference<>(
            Collections.newSetFromMap(new IdentityHashMap<>()));
    private static final AtomicReference<Set<Closeable>> providerCloseables = new AtomicReference<>(
            Collections.newSetFromMap(new IdentityHashMap<>()));

    @BuildStep
    public DevServicesRestClientProxyProvider.BuildItem registerDefaultProvider() {
        return new DevServicesRestClientProxyProvider.BuildItem(VertxHttpProxyDevServicesRestClientProxyProvider.INSTANCE);
    }

    @BuildStep
    public void determineRequiredProxies(
            RestClientsBuildTimeConfigBuildItem restClientBuildTimeConfig,
            BuildProducer<RestClientHttpProxyBuildItem> producer) {

        Map<String, RestClientBuildConfig> configs = restClientBuildTimeConfig.getRestClientsBuildTimeConfig().clients();
        if (configs.isEmpty()) {
            return;
        }

        for (RegisteredRestClient registeredRestClient : restClientBuildTimeConfig.getRestClients()) {
            String restClient = registeredRestClient.getFullName();

            RestClientBuildConfig restClientBuildConfig = configs.get(restClient);
            if (!restClientBuildConfig.enableLocalProxy()) {
                log.trace("Ignoring REST Client  '" + restClient + "' because enableLocalProxy is false");
                continue;
            }

            // the base URI is runtime configuration, which the application publishes to the proxy once it starts
            producer.produce(new RestClientHttpProxyBuildItem(restClient, restClientBuildConfig.localProxyProvider()));
        }
    }

    @BuildStep
    public void start(List<RestClientHttpProxyBuildItem> restClientHttpProxyBuildItems,
            List<DevServicesRestClientProxyProvider.BuildItem> restClientProxyProviderBuildItems,
            BuildProducer<DevServicesResultBuildItem> devServicePropertiesProducer,
            CuratedApplicationShutdownBuildItem closeBuildItem) {
        if (restClientHttpProxyBuildItems.isEmpty()) {
            return;
        }

        // determine which providers to use for each of the proxies
        Map<RestClientHttpProxyBuildItem, DevServicesRestClientProxyProvider> biToProviderMap = new HashMap<>();
        for (var toStart : restClientHttpProxyBuildItems) {
            DevServicesRestClientProxyProvider provider;
            if (toStart.getProvider().isPresent()) {
                String requestedProviderName = toStart.getProvider().get();

                var maybeProviderBI = restClientProxyProviderBuildItems
                        .stream()
                        .filter(pbi -> requestedProviderName.equals(pbi.getProvider().name()))
                        .findFirst();
                if (maybeProviderBI.isEmpty()) {
                    throw new RuntimeException("Unable to find provider for REST Client '" + toStart.getClassName()
                            + "' with name '" + requestedProviderName + "'");
                }

                provider = maybeProviderBI.get().getProvider();
            } else {
                // the algorithm is the following:
                // if only the default is around, use it
                // if there is only one besides the default, use it
                // if there are multiple ones, fail

                List<DevServicesRestClientProxyProvider.BuildItem> nonDefault = restClientProxyProviderBuildItems.stream()
                        .filter(pib -> !pib.getProvider().name().equals(VertxHttpProxyDevServicesRestClientProxyProvider.NAME))
                        .toList();
                if (nonDefault.isEmpty()) {
                    provider = VertxHttpProxyDevServicesRestClientProxyProvider.INSTANCE;
                } else if (nonDefault.size() == 1) {
                    // TODO: this part of the algorithm is questionable...
                    provider = nonDefault.iterator().next().getProvider();
                } else {
                    String availableProviders = restClientProxyProviderBuildItems.stream().map(bi -> bi.getProvider().name())
                            .collect(
                                    Collectors.joining(","));
                    throw new RuntimeException("Multiple providers found for REST Client '" + toStart.getClassName()
                            + "'. Please specify one by setting 'quarkus.rest-client.\"" + toStart.getClassName()
                            + "\".local-proxy-provider' to one the following providers: " + availableProviders);
                }
            }

            biToProviderMap.put(toStart, provider);
        }

        // here is where we set up providers
        var providersToRun = new HashSet<>(biToProviderMap.values());
        providersToRun.removeAll(runningProviders.get());
        for (var provider : providersToRun) {
            Closeable closeable = provider.setup();
            if (closeable != null) {
                providerCloseables.get().add(closeable);
            }
            runningProviders.get().add(provider);
        }

        // the proxies are Dev Services, started before the application and kept across restarts while unchanged
        for (var entry : biToProviderMap.entrySet()) {
            RestClientHttpProxyBuildItem bi = entry.getKey();
            DevServicesRestClientProxyProvider provider = entry.getValue();
            String urlKeyName = String.format("quarkus.rest-client.\"%s\".override-uri", bi.getClassName());

            devServicePropertiesProducer.produce(DevServicesResultBuildItem.owned()
                    .feature(Feature.REST_CLIENT)
                    .serviceName(bi.getClassName())
                    .description("rest-client-" + bi.getClassName() + "-proxy")
                    .serviceConfig(provider.name())
                    .startable(() -> new RestClientProxyStartable(provider, bi))
                    .configProvider(new Function<RestClientProxyStartable, Map<String, String>>() {
                        @Override
                        public Map<String, String> apply(RestClientProxyStartable proxy) {
                            return Map.of(urlKeyName, proxy.getConnectionInfo());
                        }
                    })
                    // the application publishes the origin on every start, once it has resolved the base URI
                    .onApplicationValue(RestClientProxyStartable.ORIGIN,
                            new BiConsumer<RestClientProxyStartable, String>() {
                                @Override
                                public void accept(RestClientProxyStartable proxy, String origin) {
                                    proxy.originAvailable(URI.create(origin));
                                }
                            })
                    .build());
        }

        closeBuildItem.addCloseTask(new CloseTask(providerCloseables, runningProviders), true);
    }

    /**
     * Publishes the origin of the proxy of each REST Client on every application start: its base URI, from the runtime
     * configuration, resolved against the addresses the application has bound, such as {@code quarkus://vertx-http}.
     * <p>
     * A separate build step from {@link #start}, which produces build items that the recorder steps binding the
     * addresses depend on, and would order these services before them.
     */
    @BuildStep
    void publishOrigins(List<RestClientHttpProxyBuildItem> restClientHttpProxyBuildItems,
            ApplicationInstanceIdBuildItem applicationId,
            ServiceRegistrar serviceRegistrar) {
        // Same id the Dev Services registry keys the running proxies with
        String applicationInstanceId = applicationId.getUUID().toString();
        String feature = Feature.REST_CLIENT.getName();
        for (RestClientHttpProxyBuildItem proxy : restClientHttpProxyBuildItems) {
            String restClient = proxy.getClassName();
            serviceRegistrar.forService("io.quarkus.rest-client.local-proxy." + restClient)
                    .atPhase(Phase.BINDING)
                    .require(RunningDevServicesRegistry.class)
                    .require(RestClientsConfig.class)
                    .consumeAll(Address.class)
                    .onStart((ctx, registry, restClientsConfig, addresses) -> {
                        RestClientConfig restClientConfig = restClientsConfig.clients().get(restClient);
                        Optional<String> baseUri = restClientConfig.uri().isPresent() ? restClientConfig.uri()
                                : restClientConfig.url();
                        if (baseUri.isEmpty()) {
                            return;
                        }
                        String resolved = AddressResolver
                                .resolve(name -> Optional.ofNullable(addresses.get(name)), baseUri.get()).toString();
                        registry.publish(applicationInstanceId, feature, restClient, RestClientProxyStartable.ORIGIN, resolved);
                    });
        }
    }

    private static class CloseTask implements Runnable {

        private final AtomicReference<Set<Closeable>> providerCloseablesRef;
        private final AtomicReference<Set<DevServicesRestClientProxyProvider>> runningProvidersRef;

        public CloseTask(
                AtomicReference<Set<Closeable>> providerCloseablesRef,
                AtomicReference<Set<DevServicesRestClientProxyProvider>> runningProvidersRef) {
            this.providerCloseablesRef = providerCloseablesRef;
            this.runningProvidersRef = runningProvidersRef;
        }

        @Override
        public void run() {
            Set<Closeable> providerCloseables = providerCloseablesRef.get();
            for (Closeable closeable : providerCloseables) {
                try {
                    if (closeable != null) {
                        log.debug("Attempting to close provider");
                        closeable.close();
                        log.debug("Closed provider");
                    }
                } catch (IOException e) {
                    throw new UncheckedException(e);
                }
            }
            providerCloseablesRef.set(Collections.newSetFromMap(new IdentityHashMap<>()));

            runningProvidersRef.set(Collections.newSetFromMap(new IdentityHashMap<>()));
        }
    }
}
