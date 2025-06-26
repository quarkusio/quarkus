package io.quarkus.rest.client.reactive.deployment.devservices;

import static io.quarkus.rest.client.reactive.deployment.RegisteredRestClientBuildItem.toRegisteredRestClients;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.UncheckedException;
import org.jboss.logging.Logger;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.rest.client.reactive.deployment.RegisteredRestClientBuildItem;
import io.quarkus.rest.client.reactive.spi.DevServicesRestClientProxyProvider;
import io.quarkus.rest.client.reactive.spi.RestClientHttpProxyBuildItem;
import io.quarkus.restclient.config.RegisteredRestClient;
import io.quarkus.restclient.config.RestClientsBuildTimeConfig;
import io.quarkus.restclient.config.RestClientsBuildTimeConfig.RestClientBuildConfig;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.smallrye.config.SmallRyeConfig;

@BuildSteps(onlyIfNot = IsNormal.class)
public class DevServicesRestClientHttpProxyProcessor {

    private static final Logger log = Logger.getLogger(DevServicesRestClientHttpProxyProcessor.class);

    // the following fields are needed for state management as proxied can come and go
    private static final AtomicReference<Set<RestClientHttpProxyBuildItem>> runningProxies = new AtomicReference<>(
            new HashSet<>());
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
            RestClientsBuildTimeConfig clientsConfig,
            CombinedIndexBuildItem combinedIndexBuildItem,
            ResourceScanningResultBuildItem resourceScanningResultBuildItem,
            List<RegisteredRestClientBuildItem> registeredRestClientBuildItems,
            BuildProducer<RestClientHttpProxyBuildItem> producer) {

        List<RegisteredRestClient> registeredRestClients = new ArrayList<>(
                toRegisteredRestClients(registeredRestClientBuildItems));
        resourceScanningResultBuildItem.getResult().getClientInterfaces().forEach((restClient, path) -> {
            if (registeredRestClients.stream()
                    .noneMatch(registeredRestClient -> registeredRestClient.getFullName().equals(restClient.toString()))) {
                registeredRestClients.add(new RegisteredRestClient(restClient.toString(), restClient.withoutPackagePrefix()));
            }
            ;
        });

        Map<String, RestClientBuildConfig> configs = clientsConfig.get(registeredRestClients).clients();
        if (configs.isEmpty()) {
            return;
        }

        SmallRyeConfig config = clientsConfig.getConfig(registeredRestClients);
        for (RegisteredRestClient registeredRestClient : registeredRestClients) {
            String restClient = registeredRestClient.getFullName();

            RestClientBuildConfig restClientBuildConfig = configs.get(restClient);
            if (!restClientBuildConfig.enableLocalProxy()) {
                log.trace("Ignoring REST Client  '" + restClient + "' because enableLocalProxy is false");
                continue;
            }

            // TODO - DevServices - We should do this, because we are querying runtime configuration
            Optional<String> baseUri = oneOf(
                    config.getOptionalValue("quarkus.rest-client." + "\"" + restClient + "\"" + ".uri", String.class),
                    config.getOptionalValue("quarkus.rest-client." + "\"" + restClient + "\"" + ".url", String.class));
            if (baseUri.isEmpty()) {
                log.debug("Unable to determine uri or url for REST Client '" + restClient + "'");
                continue;
            }
            producer.produce(
                    new RestClientHttpProxyBuildItem(restClient, baseUri.get(), restClientBuildConfig.localProxyProvider()));
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

        Set<RestClientHttpProxyBuildItem> requestedProxies = new HashSet<>(restClientHttpProxyBuildItems);

        Set<RestClientHttpProxyBuildItem> proxiesToClose = new HashSet<>(runningProxies.get());
        proxiesToClose.removeAll(requestedProxies);

        // we need to remove the running ones that should no longer be running
        for (var running : proxiesToClose) {
            closeRunningProxy(running);
        }
        runningProxies.get().removeAll(proxiesToClose);

        // we need to figure out which ones to start
        Set<RestClientHttpProxyBuildItem> proxiesToRun = new HashSet<>(requestedProxies);
        proxiesToRun.removeAll(runningProxies.get());

        // determine which providers to use for each of the new proxies to start
        Map<RestClientHttpProxyBuildItem, DevServicesRestClientProxyProvider> biToProviderMap = new HashMap<>();
        for (var toStart : proxiesToRun) {
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

        // this is where we actually start proxies
        for (var bi : proxiesToRun) {
            URI baseUri = URI.create(bi.getBaseUri());

            var provider = biToProviderMap.get(bi);
            var createResult = provider.create(bi);
            var proxyServerClosable = createResult.closeable();
            bi.attachClosable(proxyServerClosable);
            runningProxies.get().add(bi);

            var urlKeyName = String.format("quarkus.rest-client.\"%s\".override-uri", bi.getClassName());
            var urlKeyValue = String.format("http://%s:%d", createResult.host(), createResult.port());
            String basePath = baseUri.getPath();
            if ((basePath != null) && !basePath.isEmpty()) {
                if (basePath.startsWith("/")) {
                    basePath = basePath.substring(1);
                }
                urlKeyValue = urlKeyValue + "/" + basePath;
            }

            devServicePropertiesProducer.produce(
                    DevServicesResultBuildItem.discovered()
                            .feature(Feature.REST_CLIENT)
                            .description("rest-client-" + bi.getClassName() + "-proxy")
                            .config(Map.of(urlKeyName, urlKeyValue))
                            .build());
        }

        closeBuildItem.addCloseTask(new CloseTask(runningProxies, providerCloseables, runningProviders), true);
    }

    private static void closeRunningProxy(RestClientHttpProxyBuildItem running) {
        try {
            Closeable closeable = running.getCloseable();
            if (closeable != null) {
                log.debug("Attempting to close HTTP proxy server for REST Client '" + running.getClassName() + "'");
                closeable.close();
                log.debug("Closed HTTP proxy server for REST Client '" + running.getClassName() + "'");
            }
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    @SafeVarargs
    private static <T> Optional<T> oneOf(Optional<T>... optionals) {
        for (Optional<T> o : optionals) {
            if (o != null && o.isPresent()) {
                return o;
            }
        }
        return Optional.empty();
    }

    private static class CloseTask implements Runnable {

        private final AtomicReference<Set<RestClientHttpProxyBuildItem>> runningProxiesRef;
        private final AtomicReference<Set<Closeable>> providerCloseablesRef;
        private final AtomicReference<Set<DevServicesRestClientProxyProvider>> runningProvidersRef;

        public CloseTask(AtomicReference<Set<RestClientHttpProxyBuildItem>> runningProxiesRef,
                AtomicReference<Set<Closeable>> providerCloseablesRef,
                AtomicReference<Set<DevServicesRestClientProxyProvider>> runningProvidersRef) {

            this.runningProxiesRef = runningProxiesRef;
            this.providerCloseablesRef = providerCloseablesRef;
            this.runningProvidersRef = runningProvidersRef;
        }

        @Override
        public void run() {
            Set<RestClientHttpProxyBuildItem> restClientHttpProxyBuildItems = runningProxiesRef.get();
            for (var bi : restClientHttpProxyBuildItems) {
                closeRunningProxy(bi);
            }
            runningProxiesRef.set(new HashSet<>());

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
