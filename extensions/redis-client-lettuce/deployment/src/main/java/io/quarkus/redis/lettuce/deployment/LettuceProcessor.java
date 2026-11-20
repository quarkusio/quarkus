package io.quarkus.redis.lettuce.deployment;

import static io.quarkus.redis.runtime.client.config.RedisConfig.DEFAULT_CLIENT_NAME;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.AsyncCloseable;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.core.resource.ClientResources;
import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ExcludeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.deployment.client.RedisDataSourceProviderBuildItem;
import io.quarkus.redis.deployment.client.RequestedRedisClientBuildItem;
import io.quarkus.redis.lettuce.runtime.internal.LettuceClientResources;
import io.quarkus.redis.lettuce.runtime.internal.LettuceClientSetup;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionFactory;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionPool;
import io.quarkus.redis.lettuce.runtime.internal.LettuceRecorder;
import io.quarkus.redis.lettuce.runtime.internal.LettuceSharedConnection;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceBlockingRedisDataSourceImpl;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceReactiveRedisDataSourceImpl;
import io.quarkus.redis.runtime.client.config.RedisClientConfig;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;

/**
 * Registers the Lettuce-backed Redis beans as synthetic beans.
 * <p>
 * For every Redis client requested through a {@link RedisDataSource} or {@link ReactiveRedisDataSource}
 * injection point (see {@link RequestedRedisClientBuildItem}), the data source beans are backed by Lettuce
 * instead of the Vert.x Redis client; the {@link RedisDataSourceProviderBuildItem} tells the
 * {@code quarkus-redis-client} extension to skip its own data source beans.
 * <p>
 * The data sources open their connection with their first command, not at startup.
 * <p>
 * Additionally, for every client injected through a Lettuce type, the following beans are produced, the connection
 * beans only when a connection type is injected for the client: their connection is opened at startup.
 * <ul>
 * <li>{@code io.lettuce.core.RedisClient} and {@code io.lettuce.core.api.StatefulRedisConnection<String, String>},
 * active when the client is not configured as a cluster;</li>
 * <li>{@code io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection<String, String>}, active when the
 * client is configured as a Sentinel or replication client ({@code client-type=sentinel} or {@code replication});
 * it is the same connection as the {@code StatefulRedisConnection} bean;</li>
 * <li>{@code io.lettuce.core.cluster.RedisClusterClient} and
 * {@code io.lettuce.core.cluster.api.StatefulRedisClusterConnection<String, String>}, active when the client is
 * configured as a cluster ({@code client-type=cluster}).</li>
 * </ul>
 * And a shared {@code io.lettuce.core.resource.ClientResources} backed by the Vert.x event loops.
 * <p>
 * Every bean reads its instance from a {@code ServiceRegistrar} service, which owns its lifecycle (see {@link #init}).
 */
public class LettuceProcessor {

    private static final String FEATURE = "redis-client-lettuce";
    private static final String REDIS_CODECS_SERVICE = "io.quarkus.redis.codecs";
    private static final String MASTER_REPLICA = "master-replica";

    // The classes holding the factories of Lettuce's reactive command APIs, see LettuceSubstitutions
    private static final List<String> REACTIVE_FACTORY_HOLDERS = List.of(
            "io.lettuce.core.api.reactive.RedisReactiveCommands$FactoryHolder",
            "io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands$FactoryHolder",
            "io.lettuce.core.cluster.pubsub.api.reactive.RedisClusterPubSubReactiveCommands$FactoryHolder",
            "io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands$FactoryHolder",
            "io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands$FactoryHolder");

    private static final DotName REDIS_CLIENT_ANNOTATION = DotName.createSimple(RedisClientName.class.getName());

    private static final DotName LETTUCE_REDIS_CLIENT = DotName.createSimple("io.lettuce.core.RedisClient");
    private static final DotName LETTUCE_STATEFUL_CONNECTION = DotName
            .createSimple("io.lettuce.core.api.StatefulRedisConnection");
    private static final DotName LETTUCE_REDIS_CLUSTER_CLIENT = DotName
            .createSimple("io.lettuce.core.cluster.RedisClusterClient");
    private static final DotName LETTUCE_STATEFUL_CLUSTER_CONNECTION = DotName
            .createSimple("io.lettuce.core.cluster.api.StatefulRedisClusterConnection");
    private static final DotName LETTUCE_STATEFUL_MASTER_REPLICA_CONNECTION = DotName
            .createSimple("io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection");
    private static final DotName LETTUCE_CLIENT_RESOURCES = DotName.createSimple("io.lettuce.core.resource.ClientResources");

    private static final Type STATEFUL_CONNECTION_STRING_STRING = stringString(LETTUCE_STATEFUL_CONNECTION);
    private static final Type STATEFUL_CLUSTER_CONNECTION_STRING_STRING = stringString(LETTUCE_STATEFUL_CLUSTER_CONNECTION);
    private static final Type STATEFUL_MASTER_REPLICA_CONNECTION_STRING_STRING = stringString(
            LETTUCE_STATEFUL_MASTER_REPLICA_CONNECTION);

    private static final Set<DotName> LETTUCE_CONNECTION_TYPES = Set.of(
            LETTUCE_STATEFUL_CONNECTION,
            LETTUCE_STATEFUL_CLUSTER_CONNECTION,
            LETTUCE_STATEFUL_MASTER_REPLICA_CONNECTION);

    private static final List<DotName> LETTUCE_INJECTION_TYPES = List.of(
            LETTUCE_REDIS_CLIENT,
            LETTUCE_STATEFUL_CONNECTION,
            LETTUCE_REDIS_CLUSTER_CLIENT,
            LETTUCE_STATEFUL_CLUSTER_CONNECTION,
            LETTUCE_STATEFUL_MASTER_REPLICA_CONNECTION,
            LETTUCE_CLIENT_RESOURCES);

    private static Type stringString(DotName connectionType) {
        return ParameterizedType.create(
                connectionType,
                new Type[] {
                        ClassType.create(DotName.createSimple("java.lang.String")),
                        ClassType.create(DotName.createSimple("java.lang.String"))
                },
                null);
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    RedisDataSourceProviderBuildItem provideDataSources() {
        return new RedisDataSourceProviderBuildItem(FEATURE);
    }

    @BuildStep
    public void registerNativeImageHints(
            BuildProducer<ExcludeConfigBuildItem> excludeConfig,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit) {
        // Lettuce ships a native-image.properties that lists DefaultCommandLatencyCollector and its pause
        // detector wrappers for build-time initialization. Initializing DefaultPauseDetectorWrapper links
        // org.LatencyUtils.PauseDetector, so with the optional LatencyUtils and HdrHistogram absent (they are
        // not dependencies of this extension) the image build fails. Without the vendor file Quarkus's own
        // defaults apply, and the collector is never constructed because the client resources configure the
        // disabled latency recorder. Do NOT pin these classes to runtime initialization: that compiles the
        // <clinit> into the image and makes --link-at-build-time reject the DefaultPauseDetectorWrapper
        // reference.
        excludeConfig.produce(new ExcludeConfigBuildItem("io\\.lettuce\\.lettuce-core",
                "/META-INF/native-image/io\\.lettuce/lettuce-core/native-image\\.properties"));
        // RedisClient.create() is folded at build time, which transitively materializes a
        // DefaultClientResources.Builder holding DefaultClientResources.DEFAULT_ADDRESS_RESOLVER_GROUP, a
        // DnsAddressResolverGroup whose DnsNameResolverBuilder is runtime-init by default. Defer the factory
        // and its supporting classes to runtime init. (Lettuce 7.x builds that group directly in
        // DefaultClientResources; the former AddressResolverGroupProvider holder class no longer exists.)
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("io.lettuce.core.RedisClient"));
        // RedisClusterClient.create() is folded the same way for the cluster client type.
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("io.lettuce.core.cluster.RedisClusterClient"));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("io.lettuce.core.resource.DefaultClientResources"));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("io.lettuce.core.resource.DefaultClientResources$Builder"));
        // The static initializers of the FactoryHolder classes create the factories of the reactive command APIs by
        // calling newFactory(), which LettuceSubstitutions replaces when Project Reactor is missing. Initialized at
        // build time, they would run the original newFactory() and store its factory, which creates the Reactor-based
        // implementations, in the image heap. Unconditional: the substitutions decide whether it matters.
        for (String holder : REACTIVE_FACTORY_HOLDERS) {
            runtimeInit.produce(new RuntimeInitializedClassBuildItem(holder));
        }
        // Lettuce's transport providers read their io.lettuce.core.epoll/kqueue/iouring switches when they initialize.
        // LettuceTransport sets the switches at startup from the transport Vert.x selected, so the providers must
        // initialize at runtime, after that, in native images too.
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("io.lettuce.core.resource.EpollProvider"));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("io.lettuce.core.resource.KqueueProvider"));
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("io.lettuce.core.resource.IOUringProvider"));
    }

    @BuildStep
    public void detectLettuceUsage(BuildProducer<RequestedLettuceClientBuildItem> requestLettuce,
            BeanDiscoveryFinishedBuildItem beans) {

        Set<String> names = new HashSet<>();
        // the clients a connection type is injected for: the others get no connection beans, so that injecting
        // a RedisClient, a RedisClusterClient or the ClientResources does not open a connection at startup
        Set<String> connectionNames = new HashSet<>();

        for (InjectionPointInfo ip : beans.getInjectionPoints()) {
            DotName type = ip.getRequiredType().name();
            if (!LETTUCE_INJECTION_TYPES.contains(type)) {
                continue;
            }
            AnnotationInstance clientName = ip.getRequiredQualifier(REDIS_CLIENT_ANNOTATION);
            String name;
            if (clientName != null) {
                name = clientName.value().asString();
            } else if (ip.hasDefaultedQualifier()) {
                name = DEFAULT_CLIENT_NAME;
            } else {
                continue;
            }
            names.add(name);
            if (LETTUCE_CONNECTION_TYPES.contains(type)) {
                connectionNames.add(name);
            }
        }

        // Produce only the Lettuce request item. We deliberately do NOT produce a
        // RequestedRedisClientBuildItem here: that item drives the Vert.x processors, which would
        // otherwise create a Vert.x Redis client for every Lettuce injection. The Lettuce beans own
        // their own lifecycle.
        for (String name : names) {
            requestLettuce.produce(new RequestedLettuceClientBuildItem(name, connectionNames.contains(name)));
        }
    }

    /**
     * Registers the services creating the Lettuce clients, connections, pools and data sources, and the synthetic
     * beans reading their values.
     * <p>
     * The services run at {@link Phase#DATA}, like the Vert.x Redis client's, so that their values exist before any
     * bean is accessed; the ones of an inactive client (see {@link LettuceRecorder#checkActive}) are absent, and the
     * beans inactive. As with the Vert.x client, a {@code RedisHostsProvider} bean is called from that phase, before
     * the synthetic beans initialized at runtime are usable (the ArC step initializing them runs after the services
     * backing the beans registered here, so the clients cannot be ordered after it). Still a {@code @Record} step:
     * {@code checkActive} needs a recorder.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public ServiceStartBuildItem init(LettuceRecorder recorder,
            ServiceRegistrar reg,
            List<RequestedLettuceClientBuildItem> lettuceClients,
            List<RequestedRedisClientBuildItem> dataSourceClients,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        Set<String> lettuceNames = new HashSet<>();
        Set<String> connectionNames = new HashSet<>();
        for (RequestedLettuceClientBuildItem client : lettuceClients) {
            lettuceNames.add(client.name);
            if (client.connection) {
                connectionNames.add(client.name);
            }
        }
        Set<String> dataSourceNames = new HashSet<>();
        for (RequestedRedisClientBuildItem client : dataSourceClients) {
            dataSourceNames.add(client.name);
        }
        if (lettuceNames.isEmpty() && dataSourceNames.isEmpty()) {
            return new ServiceStartBuildItem(FEATURE);
        }

        // The shared client resources, on the Vert.x event loops: created as soon as any client exists, shut down
        // after the clients built on them (which depend on them) and before Vert.x closes its event loops.
        reg.forService(LettuceClientResources.class)
                .atPhase(Phase.DATA)
                .require(Vertx.class)
                .onStart((ctx, vertx) -> {
                    LettuceClientResources resources = LettuceClientSetup.createResources(vertx);
                    ctx.onStop(resources::shutdown);
                    return resources;
                });
        reg.forService(ClientResources.class)
                .atPhase(Phase.DATA)
                .require(LettuceClientResources.class)
                .onStart((ctx, resources) -> resources.clientResources());

        Set<String> names = new HashSet<>(lettuceNames);
        names.addAll(dataSourceNames);
        for (String name : names) {
            registerClientService(reg, name);
        }
        for (String name : lettuceNames) {
            boolean connection = connectionNames.contains(name);
            registerLettuceServices(reg, name, connection);
            registerLettuceBeans(syntheticBeans, recorder, name, connection);
        }
        for (String name : dataSourceNames) {
            registerDataSourceServices(reg, name);
            registerDataSourceBeans(syntheticBeans, recorder, name);
        }

        // Shared ClientResources bean (singleton, default qualifier). The resources are created as soon as any
        // client exists, so the bean is always active rather than tied to the activation state of the default client.
        syntheticBeans.produce(createLettuceBean(DEFAULT_CLIENT_NAME, ClientResources.class,
                ClassType.create(LETTUCE_CLIENT_RESOURCES), null, ClientResources.class, List.of(),
                ApplicationScoped.class));

        return new ServiceStartBuildItem(FEATURE);
    }

    /**
     * The service creating the Lettuce client of a client name, absent when the client is inactive. Every other
     * service of the client requests it.
     */
    private static void registerClientService(ServiceRegistrar reg, String name) {
        reg.forService(LettuceConnectionFactory.class, name)
                .atPhase(Phase.DATA)
                .require(LettuceClientResources.class)
                .require(RedisConfig.class)
                .require(Vertx.class)
                .require(TlsConfigurationRegistry.class)
                .require(BeanContainer.class)
                .optional()
                .onStart((ctx, resources, config, vertx, tlsRegistry, beanContainer) -> {
                    RedisClientConfig clientConfig = config.clients().get(name);
                    if (!LettuceClientSetup.isActive(clientConfig)) {
                        return Optional.empty();
                    }
                    LettuceConnectionFactory factory = LettuceClientSetup.createFactory(name, clientConfig, resources,
                            vertx, tlsRegistry);
                    ctx.onStop(factory::shutdown);
                    return Optional.of(factory);
                });
    }

    /**
     * The services behind the Lettuce beans of a client: its {@code RedisClient} or {@code RedisClusterClient}, and,
     * when a connection type is injected for the client, the {@code String} connection the three connection beans
     * share (the beans of the other topologies being inactive, see {@link #registerLettuceBeans}), registered under
     * {@link AsyncCloseable} and opened at startup.
     */
    private static void registerLettuceServices(ServiceRegistrar reg, String name, boolean connection) {
        reg.forService(RedisClient.class, name)
                .atPhase(Phase.DATA)
                .request(LettuceConnectionFactory.class, name)
                .optional()
                .onStart((ctx, factoryOpt) -> factoryOpt.flatMap(LettuceClientSetup::redisClient));
        reg.forService(RedisClusterClient.class, name)
                .atPhase(Phase.DATA)
                .request(LettuceConnectionFactory.class, name)
                .optional()
                .onStart((ctx, factoryOpt) -> factoryOpt.flatMap(LettuceClientSetup::clusterClient));
        if (!connection) {
            return;
        }
        // A service type cannot be generic, and every Lettuce connection type is: the connection is registered under
        // AsyncCloseable, the non-generic Lettuce supertype of all of them, and the beans cast it.
        reg.forService(AsyncCloseable.class, name)
                .atPhase(Phase.DATA)
                .request(LettuceConnectionFactory.class, name)
                .optional()
                .onStart((ctx, factoryOpt) -> factoryOpt.map(factory -> {
                    StatefulConnection<String, String> strings = LettuceClientSetup.connectStrings(name, factory);
                    ctx.onStop(strings::close);
                    return strings;
                }));
        // A bean takes its value out of the service graph on first access, so two beans cannot read the same service:
        // the master/replica connection bean, active together with the StatefulRedisConnection one, reads the same
        // connection through a service of its own.
        reg.forService(AsyncCloseable.class, List.of(name, MASTER_REPLICA))
                .atPhase(Phase.DATA)
                .request(AsyncCloseable.class, name)
                .optional()
                .onStart((ctx, connectionOpt) -> connectionOpt);
    }

    /**
     * The services behind the data source beans of a client: the pool of its data sources, the reactive data source
     * and the blocking one wrapping it. Starting them connects to nothing: the reactive data source owns the shared
     * connection, which its first command opens, and the pool opens its connections when they are borrowed.
     */
    private static void registerDataSourceServices(ServiceRegistrar reg, String name) {
        reg.forService(LettuceConnectionPool.class, name)
                .atPhase(Phase.DATA)
                .request(LettuceConnectionFactory.class, name)
                .require(RedisConfig.class)
                .require(Vertx.class)
                .optional()
                .onStart((ctx, factoryOpt, config, vertx) -> factoryOpt.map(factory -> {
                    LettuceConnectionPool pool = LettuceClientSetup.createPool(config.clients().get(name), factory, vertx);
                    ctx.onStop(() -> LettuceClientSetup.closePool(pool));
                    return pool;
                }));
        // after the codecs of the application are registered, as the Vert.x data source (see RedisDatasourceProcessor)
        reg.forService(ReactiveRedisDataSource.class, name)
                .atPhase(Phase.DATA)
                .after(REDIS_CODECS_SERVICE)
                .request(LettuceConnectionPool.class, name)
                .request(LettuceConnectionFactory.class, name)
                .require(Vertx.class)
                .optional()
                .onStart((ctx, poolOpt, factoryOpt, vertx) -> factoryOpt.map(factory -> {
                    LettuceSharedConnection connection = LettuceClientSetup.sharedConnection(name, factory);
                    ctx.onStop(connection::close);
                    return new LettuceReactiveRedisDataSourceImpl(io.vertx.mutiny.core.Vertx.newInstance(vertx),
                            connection, poolOpt.orElseThrow(), factory::connectPubSubAsync);
                }));
        reg.forService(RedisDataSource.class, name)
                .atPhase(Phase.DATA)
                .request(ReactiveRedisDataSource.class, name)
                .require(RedisConfig.class)
                .optional()
                .onStart((ctx, dataSourceOpt, config) -> dataSourceOpt
                        .map(dataSource -> new LettuceBlockingRedisDataSourceImpl(
                                (LettuceReactiveRedisDataSourceImpl) dataSource, config.clients().get(name).timeout())));
    }

    private static void registerLettuceBeans(BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            LettuceRecorder recorder, String name, boolean connection) {
        // The client type is runtime configuration: the standalone, cluster and master/replica beans are all
        // registered, and the ones of the other topologies are inactive, with a message naming the types to inject
        // instead.
        Supplier<ActiveResult> checkActiveStandalone = recorder.checkActiveStandalone(name);
        Supplier<ActiveResult> checkActiveCluster = recorder.checkActiveCluster(name);

        syntheticBeans.produce(
                createLettuceBean(name, RedisClient.class, ClassType.create(LETTUCE_REDIS_CLIENT),
                        checkActiveStandalone, RedisClient.class, List.of(name), ApplicationScoped.class));
        // A singleton, unlike the other Lettuce beans: the client proxy ArC would generate for an application
        // scoped RedisClusterClient lives in io.lettuce.core.cluster and overrides methods whose signatures
        // name the package-private io.lettuce.core.RedisHandshake, which the native image build rejects when
        // it links the proxy (the RedisClient proxy is fine, being in the same package as RedisHandshake).
        syntheticBeans.produce(
                createLettuceBean(name, RedisClusterClient.class, ClassType.create(LETTUCE_REDIS_CLUSTER_CLIENT),
                        checkActiveCluster, RedisClusterClient.class, List.of(name), Singleton.class));
        if (!connection) {
            // no connection type is injected for the client: no connection service to read (see
            // registerLettuceServices), and nothing to open at startup
            return;
        }
        Supplier<ActiveResult> checkActiveMasterReplica = recorder.checkActiveMasterReplica(name);
        syntheticBeans.produce(
                createLettuceBean(name, StatefulRedisConnection.class, STATEFUL_CONNECTION_STRING_STRING,
                        checkActiveStandalone, AsyncCloseable.class, List.of(name), ApplicationScoped.class));
        syntheticBeans.produce(
                createLettuceBean(name, StatefulRedisClusterConnection.class, STATEFUL_CLUSTER_CONNECTION_STRING_STRING,
                        checkActiveCluster, AsyncCloseable.class, List.of(name), ApplicationScoped.class));
        syntheticBeans.produce(
                createLettuceBean(name, StatefulRedisMasterReplicaConnection.class,
                        STATEFUL_MASTER_REPLICA_CONNECTION_STRING_STRING, checkActiveMasterReplica,
                        AsyncCloseable.class, List.of(name, MASTER_REPLICA), ApplicationScoped.class));
    }

    private static void registerDataSourceBeans(BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            LettuceRecorder recorder, String name) {
        Supplier<ActiveResult> checkActive = recorder.checkActive(name);
        syntheticBeans.produce(createDataSourceBean(name, RedisDataSource.class, checkActive));
        syntheticBeans.produce(createDataSourceBean(name, ReactiveRedisDataSource.class, checkActive));
    }

    /**
     * Creates a Lettuce synthetic bean with the given type, checkActive guard (a recorder proxy, or {@code null}
     * for an always-active bean) and scope, reading its instance from the service of the given type and name parts
     * (none for an unnamed service).
     */
    static SyntheticBeanBuildItem createLettuceBean(String name, Class<?> implClass, Type beanType,
            Supplier<ActiveResult> checkActive, Class<?> serviceType, List<String> serviceNameParts,
            Class<? extends Annotation> scope) {

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(implClass)
                .addType(beanType)
                .startup()
                .setRuntimeInit()
                .unremovable()
                .serviceValue(serviceType, serviceNameParts)
                .scope(scope);
        if (checkActive != null) {
            configurator.checkActive(checkActive);
        }

        return qualify(configurator, name).done();
    }

    /**
     * Creates a data source synthetic bean, mirroring the configuration of the Vert.x-backed ones: its instance is
     * the value of the service of the same type and name.
     */
    static <T> SyntheticBeanBuildItem createDataSourceBean(String name, Class<T> type,
            Supplier<ActiveResult> checkActive) {

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(type)
                .checkActive(checkActive)
                .startup()
                .setRuntimeInit()
                .unremovable()
                .serviceValue(type, name)
                .scope(ApplicationScoped.class);

        return qualify(configurator, name).done();
    }

    private static SyntheticBeanBuildItem.ExtendedBeanConfigurator qualify(
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator, String name) {
        if (DEFAULT_CLIENT_NAME.equalsIgnoreCase(name)) {
            configurator.addQualifier(Default.class);
        } else {
            configurator.addQualifier().annotation(REDIS_CLIENT_ANNOTATION).addValue("value", name).done();
        }
        return configurator;
    }
}
