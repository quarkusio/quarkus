package io.quarkus.redis.deployment.client;

import static io.quarkus.redis.runtime.client.config.RedisConfig.DEFAULT_CLIENT_NAME;
import static io.quarkus.redis.runtime.client.config.RedisConfig.HOSTS;
import static io.quarkus.redis.runtime.client.config.RedisConfig.HOSTS_PROVIDER_NAME;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.proxy.ProxyConfigurationRegistry;
import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.client.RedisHostsProvider;
import io.quarkus.redis.client.RedisOptionsCustomizer;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.quarkus.redis.runtime.client.ObservableRedisMetrics;
import io.quarkus.redis.runtime.client.RedisClientConnection;
import io.quarkus.redis.runtime.client.RedisClientRecorder;
import io.quarkus.redis.runtime.client.RedisDataLoader;
import io.quarkus.redis.runtime.client.VertxRedisClientFactory;
import io.quarkus.redis.runtime.client.config.RedisClientConfig;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.configuration.NameIterator;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.config.SmallRyeConfig;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.impl.types.BulkType;

public class RedisClientProcessor {

    static final DotName REDIS_CLIENT_ANNOTATION = DotName.createSimple(RedisClientName.class.getName());

    private static final String FEATURE = "redis-client";

    private static final List<DotName> SUPPORTED_INJECTION_TYPE = List.of(
            // Legacy types
            DotName.createSimple(RedisClient.class.getName()),
            DotName.createSimple(ReactiveRedisClient.class.getName()),
            // Client types
            DotName.createSimple(io.vertx.mutiny.redis.client.Redis.class.getName()),
            DotName.createSimple(io.vertx.mutiny.redis.client.RedisAPI.class.getName()),
            DotName.createSimple(io.vertx.redis.client.Redis.class.getName()),
            DotName.createSimple(io.vertx.redis.client.RedisAPI.class.getName()));

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public void registerRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> producer) {
        producer.produce(new RuntimeInitializedClassBuildItem(BulkType.class.getName()));
        // Classes using SplittableRandom, which need to be runtime initialized
        producer.produce(new RuntimeInitializedClassBuildItem("io.vertx.redis.client.impl.RedisSentinelClient"));
        producer.produce(new RuntimeInitializedClassBuildItem("io.vertx.redis.client.impl.RedisReplicationClient"));
        producer.produce(new RuntimeInitializedClassBuildItem("io.vertx.redis.client.impl.Slots"));
        producer.produce(new RuntimeInitializedClassBuildItem("io.vertx.redis.client.impl.RedisClusterConnection"));
        producer.produce(new RuntimeInitializedClassBuildItem("io.vertx.redis.client.impl.RedisReplicationConnection"));
        // RedisClusterConnections is referenced from RedisClusterClient. Thus, we need to runtime-init that too.
        producer.produce(new RuntimeInitializedClassBuildItem("io.vertx.redis.client.impl.RedisClusterClient"));
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    List<AdditionalBeanBuildItem> registerRedisClientName() {
        List<AdditionalBeanBuildItem> list = new ArrayList<>();
        list.add(AdditionalBeanBuildItem
                .builder()
                .addBeanClass(RedisClientName.class)
                .build());
        return list;
    }

    @BuildStep
    UnremovableBeanBuildItem makeHostsProviderAndOptionsCustomizerUnremovable() {
        return UnremovableBeanBuildItem.beanTypes(RedisHostsProvider.class, RedisOptionsCustomizer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void init(
            List<RequestedRedisClientBuildItem> clients,
            RedisClientRecorder recorder,
            ServiceRegistrar reg,
            RedisBuildTimeConfig buildTimeConfig,
            BeanArchiveIndexBuildItem indexBuildItem,
            BeanDiscoveryFinishedBuildItem beans,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            ApplicationArchivesBuildItem applicationArchivesBuildItem, LaunchModeBuildItem launchMode,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {

        // Collect the used redis clients, the unused clients will not be instantiated.
        Set<String> names = new HashSet<>();

        // Add the names from the requested clients.
        for (RequestedRedisClientBuildItem client : clients) {
            names.add(client.name);
        }

        IndexView indexView = indexBuildItem.getIndex();
        Collection<AnnotationInstance> clientAnnotations = indexView.getAnnotations(REDIS_CLIENT_ANNOTATION);
        for (AnnotationInstance annotation : clientAnnotations) {
            names.add(annotation.value().asString());
        }

        // Check if the application use the default Redis client.
        beans.getInjectionPoints().stream().filter(InjectionPointInfo::hasDefaultedQualifier)
                .filter(i -> SUPPORTED_INJECTION_TYPE.contains(i.getRequiredType().name()))
                .findAny()
                .ifPresent(x -> names.add(DEFAULT_CLIENT_NAME));

        beans.getInjectionPoints().stream()
                .filter(i -> SUPPORTED_INJECTION_TYPE.contains(i.getRequiredType().name()))
                .filter(InjectionPointInfo::isProgrammaticLookup)
                .findAny()
                .ifPresent(x -> names.addAll(configuredClientNames(buildTimeConfig, ConfigProvider.getConfig())));

        // Register services and synthetic beans for each client name
        for (String name : names) {
            Supplier<ActiveResult> checkActive = recorder.checkActive(name);

            registerClientServices(reg, name);
            registerClientBeans(syntheticBeans, checkActive, name);
        }

        // Handle data import (chained sequentially since clients may share a server)
        String previousPreload = null;
        for (String name : buildTimeConfig.clientsNames()) {
            previousPreload = preloadRedisData(name, buildTimeConfig.clients().get(name), applicationArchivesBuildItem,
                    launchMode.getLaunchMode(),
                    nativeImageResources, hotDeploymentWatchedFiles, reg, previousPreload);
        }
    }

    /**
     * Register the core client connection service and all derived bean services
     * for a single named Redis client.
     */
    private void registerClientServices(ServiceRegistrar reg, String name) {
        // Core connection service: creates the Vert.x Redis client
        reg.forService(RedisClientConnection.class, name)
                .atPhase(Phase.DATA)
                .require(RedisConfig.class)
                .require(Vertx.class)
                .require(TlsConfigurationRegistry.class)
                .require(BeanContainer.class)
                .request(ProxyConfigurationRegistry.class)
                .optional()
                .onStart((ctx, config, vertx, tlsReg, bc, proxyRegOpt) -> {
                    RedisClientConfig clientConfig = config.clients().get(name);
                    if (!clientConfig.active()
                            || (clientConfig.hosts().isEmpty() && clientConfig.hostsProviderName().isEmpty())) {
                        return Optional.empty();
                    }
                    ObservableRedisMetrics metrics = null;
                    Instance<ObservableRedisMetrics> inst = jakarta.enterprise.inject.spi.CDI.current()
                            .select(ObservableRedisMetrics.class);
                    if (inst.isResolvable()) {
                        metrics = inst.get();
                    }
                    RedisClientConnection conn = RedisClientConnection.create(name,
                            VertxRedisClientFactory.create(name, vertx, clientConfig, tlsReg,
                                    proxyRegOpt.orElse(null)),
                            metrics);
                    ctx.onStop(() -> conn.redis().close());
                    return Optional.of(conn);
                });

        // Mutiny Redis client bean service
        reg.forService(io.vertx.mutiny.redis.client.Redis.class, name)
                .atPhase(Phase.DATA)
                .request(RedisClientConnection.class, name)
                .optional()
                .onStart((ctx, connOpt) -> connOpt.map(RedisClientConnection::redis));

        // Bare Vert.x Redis client bean service
        reg.forService(io.vertx.redis.client.Redis.class, name)
                .atPhase(Phase.DATA)
                .request(RedisClientConnection.class, name)
                .optional()
                .onStart((ctx, connOpt) -> connOpt.map(RedisClientConnection::observable));

        // Mutiny RedisAPI bean service
        reg.forService(io.vertx.mutiny.redis.client.RedisAPI.class, name)
                .atPhase(Phase.DATA)
                .request(RedisClientConnection.class, name)
                .optional()
                .onStart((ctx, connOpt) -> connOpt.map(RedisClientConnection::api));

        // Bare Vert.x RedisAPI bean service
        reg.forService(io.vertx.redis.client.RedisAPI.class, name)
                .atPhase(Phase.DATA)
                .request(RedisClientConnection.class, name)
                .optional()
                .onStart((ctx, connOpt) -> connOpt.map(c -> c.api().getDelegate()));

        // Legacy RedisClient bean service
        reg.forService(RedisClient.class, name)
                .atPhase(Phase.DATA)
                .request(RedisClientConnection.class, name)
                .require(RedisConfig.class)
                .optional()
                .onStart((ctx, connOpt, config) -> connOpt.map(conn -> {
                    java.time.Duration timeout = config.clients().get(name).timeout();
                    return new io.quarkus.redis.runtime.client.RedisClientImpl(
                            conn.redis(), conn.api(), timeout);
                }));

        // Legacy ReactiveRedisClient bean service
        reg.forService(ReactiveRedisClient.class, name)
                .atPhase(Phase.DATA)
                .request(RedisClientConnection.class, name)
                .optional()
                .onStart((ctx, connOpt) -> connOpt.map(conn -> new io.quarkus.redis.runtime.client.ReactiveRedisClientImpl(
                        conn.redis(), conn.api())));
    }

    /**
     * Register synthetic CDI beans backed by service values.
     */
    private void registerClientBeans(BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            Supplier<ActiveResult> checkActive, String name) {
        // Client types
        syntheticBeans.produce(configureAndCreateSyntheticBean(name, io.vertx.mutiny.redis.client.Redis.class, checkActive));
        syntheticBeans.produce(configureAndCreateSyntheticBean(name, io.vertx.redis.client.Redis.class, checkActive));
        syntheticBeans.produce(configureAndCreateSyntheticBean(name, io.vertx.mutiny.redis.client.RedisAPI.class, checkActive));
        syntheticBeans.produce(configureAndCreateSyntheticBean(name, io.vertx.redis.client.RedisAPI.class, checkActive));
        // Legacy types
        syntheticBeans.produce(configureAndCreateSyntheticBean(name, RedisClient.class, checkActive));
        syntheticBeans.produce(configureAndCreateSyntheticBean(name, ReactiveRedisClient.class, checkActive));
    }

    static Set<String> configuredClientNames(RedisBuildTimeConfig buildTimeConfig, Config config) {
        Set<String> names = new HashSet<>();
        // redis client names from dev services
        for (Entry<String, RedisClientBuildTimeConfig> client : buildTimeConfig.clients().entrySet()) {
            if (client.getValue().devservices().enabled()) {
                names.add(client.getKey());
            }
        }

        // TODO - We shouldn't query runtime config during deployment
        Map<String, String> map = config.unwrap(SmallRyeConfig.class).getMapKeys("quarkus.redis");
        for (Entry<String, String> entry : map.entrySet()) {
            NameIterator nameIterator = new NameIterator(entry.getKey());

            if (nameIterator.nextSegmentEquals(HOSTS) || nameIterator.nextSegmentEquals(HOSTS_PROVIDER_NAME)) {
                names.add(DEFAULT_CLIENT_NAME);
                continue;
            }

            if (nameIterator.hasNext()) {
                String candidateName = nameIterator.getNextSegment();
                nameIterator.next();
                if (nameIterator.hasNext()
                        && (nameIterator.nextSegmentEquals(HOSTS) || nameIterator.nextSegmentEquals(HOSTS_PROVIDER_NAME))) {
                    names.add(candidateName);
                }
            }
        }

        return names;
    }

    /**
     * Configure a synthetic bean backed by a service value.
     */
    static <T> SyntheticBeanBuildItem configureAndCreateSyntheticBean(String name, Class<T> type,
            Supplier<ActiveResult> checkActive) {

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(type)
                .checkActive(checkActive)
                .startup()
                .setRuntimeInit()
                .unremovable()
                .serviceValue(type, name)
                .scope(ApplicationScoped.class);

        if (DEFAULT_CLIENT_NAME.equalsIgnoreCase(name)) {
            configurator.addQualifier(Default.class);
        } else {
            configurator.addQualifier().annotation(REDIS_CLIENT_ANNOTATION).addValue("value", name).done();
        }

        return configurator.done();
    }

    /**
     * Register a preload service for the given client name, returning the service name
     * (or {@code previousPreload} if no preload was registered).
     */
    private String preloadRedisData(String name, RedisClientBuildTimeConfig clientConfig,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode, BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles, ServiceRegistrar reg,
            String previousPreload) {
        List<String> importFiles = getRedisLoadScript(clientConfig, launchMode);
        List<String> paths = new ArrayList<>();
        for (String importFile : importFiles) {
            Path loadScriptPath;
            try {
                loadScriptPath = applicationArchivesBuildItem.getRootArchive().getChildPath(importFile);
            } catch (RuntimeException e) {
                throw new ConfigurationException(
                        "Unable to interpret path referenced in '"
                                + RedisConfig.getPropertyName(name, "redis-load-script") + "="
                                + String.join(",", importFiles)
                                + "': " + e.getMessage());
            }

            if (loadScriptPath != null && !Files.isDirectory(loadScriptPath)) {
                // enlist resource if present
                nativeImageResources.produce(new NativeImageResourceBuildItem(importFile));
            } else if (clientConfig != null && clientConfig.loadScript().isPresent()) {
                //raise exception if explicit file is not present (i.e. not the default)
                throw new ConfigurationException(
                        "Unable to find file referenced in '"
                                + RedisConfig.getPropertyName(name, "redis-load-script") + "="
                                + String.join(", ", clientConfig.loadScript().get())
                                + "'. Remove property or add file to your path.");
            }
            // in dev mode we want to make sure that we watch for changes to file even if it doesn't currently exist
            // as a user could still add it after performing the initial configuration
            hotDeploymentWatchedFiles.produce(new HotDeploymentWatchedFileBuildItem(importFile));

            if (loadScriptPath != null) {
                paths.add(importFile);
            }
        }

        if (!paths.isEmpty()) {
            boolean flushBeforeLoad = clientConfig != null && clientConfig.flushBeforeLoad();
            boolean loadOnlyIfEmpty = clientConfig == null || clientConfig.loadOnlyIfEmpty();
            List<String> capturedPaths = List.copyOf(paths);
            String serviceName = "io.quarkus.redis.preload." + name;

            var builder = reg.forService(serviceName)
                    .atPhase(Phase.INIT)
                    .require(RedisClientConnection.class, name)
                    .require(Vertx.class);
            if (previousPreload != null) {
                builder.after(previousPreload);
            }
            builder.onStart((ctx, conn, vertx) -> {
                io.vertx.mutiny.core.Vertx mutinyVertx = io.vertx.mutiny.core.Vertx.newInstance(vertx);
                if (flushBeforeLoad) {
                    conn.redis().send(Request.cmd(Command.FLUSHALL)).await().indefinitely();
                } else if (loadOnlyIfEmpty) {
                    var list = conn.redis().send(Request.cmd(Command.KEYS).arg("*")).await().indefinitely();
                    if (list.size() != 0) {
                        return;
                    }
                }
                for (String path : capturedPaths) {
                    RedisDataLoader.load(mutinyVertx, conn.redis(), path);
                }
            });
            return serviceName;
        }
        return previousPreload;
    }

    @BuildStep
    HealthBuildItem addHealthCheck(RedisBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.redis.runtime.client.health.RedisHealthCheck",
                buildTimeConfig.healthEnabled());
    }

    public static final String NO_REDIS_SCRIPT_FILE = "no-file";

    private static List<String> getRedisLoadScript(RedisClientBuildTimeConfig config, LaunchMode launchMode) {
        if (config == null) {
            return List.of("import.redis");
        }
        var scripts = config.loadScript();
        if (scripts.isPresent()) {
            return scripts.get().stream()
                    .filter(s -> !NO_REDIS_SCRIPT_FILE.equalsIgnoreCase(s))
                    .collect(Collectors.toList());
        } else if (launchMode.isProduction()) {
            return Collections.emptyList();
        } else {
            return List.of("import.redis");
        }
    }
}
