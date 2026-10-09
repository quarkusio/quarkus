package io.quarkus.redis.lettuce.deployment;

import static io.quarkus.redis.runtime.client.config.RedisConfig.DEFAULT_CLIENT_NAME;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
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

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ExcludeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.deployment.client.RedisDataSourceProviderBuildItem;
import io.quarkus.redis.deployment.client.RequestedRedisClientBuildItem;
import io.quarkus.redis.lettuce.runtime.internal.LettuceRecorder;
import io.quarkus.tls.deployment.spi.TlsRegistryBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;

/**
 * Registers the Lettuce-backed Redis beans as synthetic beans.
 * <p>
 * For every Redis client requested through a {@link RedisDataSource} or {@link ReactiveRedisDataSource}
 * injection point (see {@link RequestedRedisClientBuildItem}), the data source beans are backed by Lettuce
 * instead of the Vert.x Redis client; the {@link RedisDataSourceProviderBuildItem} tells the
 * {@code quarkus-redis-client} extension to skip its own data source beans.
 * <p>
 * Additionally, for every client injected through a Lettuce type, the following beans are produced:
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
 */
public class LettuceProcessor {

    private static final String FEATURE = "redis-client-lettuce";

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
        // Lettuce ships a native-image.properties that explicitly lists DefaultCommandLatencyCollector and
        // its pause detector wrappers for build-time initialization. native-image eagerly initializes
        // explicitly listed classes, and linking DefaultPauseDetectorWrapper requires LatencyUtils and
        // HdrHistogram. Both are hard dependencies of this extension, but the vendor file must still be
        // dropped so that Quarkus's own default (build-time initialization, which only touches the
        // pause detector when both libraries are present) applies. Do NOT pin these classes to runtime
        // initialization: that compiles the <clinit> into the image and makes --link-at-build-time reject
        // the DefaultPauseDetectorWrapper reference.
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
        // calling newFactory(), which LettuceSubstitutions replaces when Reactive Streams is missing. Initialized at
        // build time, they would run the original newFactory() and store its factory, which creates the Reactor-based
        // implementations, in the image heap. Unconditional: the substitutions decide whether it matters.
        for (String holder : REACTIVE_FACTORY_HOLDERS) {
            runtimeInit.produce(new RuntimeInitializedClassBuildItem(holder));
        }
    }

    @BuildStep
    public void detectLettuceUsage(BuildProducer<RequestedLettuceClientBuildItem> requestLettuce,
            BeanDiscoveryFinishedBuildItem beans) {

        Set<String> names = new HashSet<>();

        for (InjectionPointInfo ip : beans.getInjectionPoints()) {
            if (!LETTUCE_INJECTION_TYPES.contains(ip.getRequiredType().name())) {
                continue;
            }
            AnnotationInstance clientName = ip.getRequiredQualifier(REDIS_CLIENT_ANNOTATION);
            if (clientName != null) {
                names.add(clientName.value().asString());
            } else if (ip.hasDefaultedQualifier()) {
                names.add(DEFAULT_CLIENT_NAME);
            }
        }

        // Produce only the Lettuce request item. We deliberately do NOT produce a
        // RequestedRedisClientBuildItem here: that item drives the Vert.x processors, which would
        // otherwise create a Vert.x Redis client for every Lettuce injection. The Lettuce beans own
        // their own lifecycle.
        for (String name : names) {
            requestLettuce.produce(new RequestedLettuceClientBuildItem(name));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void init(LettuceRecorder recorder,
            List<RequestedLettuceClientBuildItem> lettuceClients,
            List<RequestedRedisClientBuildItem> dataSourceClients,
            ShutdownContextBuildItem shutdown,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            VertxBuildItem vertxBuildItem,
            TlsRegistryBuildItem tlsRegistryBuildItem) {

        Set<String> lettuceNames = new HashSet<>();
        for (RequestedLettuceClientBuildItem client : lettuceClients) {
            lettuceNames.add(client.name);
        }
        Set<String> dataSourceNames = new HashSet<>();
        for (RequestedRedisClientBuildItem client : dataSourceClients) {
            dataSourceNames.add(client.name);
        }
        Set<String> names = new HashSet<>(lettuceNames);
        names.addAll(dataSourceNames);
        if (names.isEmpty()) {
            return;
        }

        // Initialize shared resources and per-client factories
        recorder.initialize(vertxBuildItem.getVertx(), tlsRegistryBuildItem.registry(), names);

        for (String name : lettuceNames) {
            // The client type is runtime configuration: the standalone, cluster and master/replica beans are all
            // registered, and the ones of the other topologies are inactive, with a message naming the types to inject
            // instead.
            Supplier<ActiveResult> checkActiveStandalone = recorder.checkActiveStandalone(name);
            Supplier<ActiveResult> checkActiveCluster = recorder.checkActiveCluster(name);
            Supplier<ActiveResult> checkActiveMasterReplica = recorder.checkActiveMasterReplica(name);

            syntheticBeans.produce(
                    createLettuceBean(name, LETTUCE_REDIS_CLIENT, ClassType.create(LETTUCE_REDIS_CLIENT),
                            checkActiveStandalone, recorder.getRedisClient(name)));
            syntheticBeans.produce(
                    createLettuceBean(name, LETTUCE_STATEFUL_CONNECTION, STATEFUL_CONNECTION_STRING_STRING,
                            checkActiveStandalone, recorder.getConnection(name)));
            // A singleton, unlike the other Lettuce beans: the client proxy ArC would generate for an application
            // scoped RedisClusterClient lives in io.lettuce.core.cluster and overrides methods whose signatures
            // name the package-private io.lettuce.core.RedisHandshake, which the native image build rejects when
            // it links the proxy (the RedisClient proxy is fine, being in the same package as RedisHandshake).
            syntheticBeans.produce(
                    createLettuceBean(name, LETTUCE_REDIS_CLUSTER_CLIENT, ClassType.create(LETTUCE_REDIS_CLUSTER_CLIENT),
                            checkActiveCluster, recorder.getClusterClient(name), Singleton.class));
            syntheticBeans.produce(
                    createLettuceBean(name, LETTUCE_STATEFUL_CLUSTER_CONNECTION, STATEFUL_CLUSTER_CONNECTION_STRING_STRING,
                            checkActiveCluster, recorder.getConnection(name)));
            syntheticBeans.produce(
                    createLettuceBean(name, LETTUCE_STATEFUL_MASTER_REPLICA_CONNECTION,
                            STATEFUL_MASTER_REPLICA_CONNECTION_STRING_STRING, checkActiveMasterReplica,
                            recorder.getConnection(name)));
        }

        for (String name : dataSourceNames) {
            Supplier<ActiveResult> checkActive = recorder.checkActive(name);

            syntheticBeans.produce(createDataSourceBean(name, RedisDataSource.class,
                    checkActive, recorder.getBlockingDataSource(name)));
            syntheticBeans.produce(createDataSourceBean(name, ReactiveRedisDataSource.class,
                    checkActive, recorder.getReactiveDataSource(name)));
        }

        // Shared ClientResources bean (singleton, default qualifier). The resources are created as soon as any
        // client exists, so the bean is always active rather than tied to the activation state of the default client.
        syntheticBeans.produce(createLettuceBean(DEFAULT_CLIENT_NAME, LETTUCE_CLIENT_RESOURCES,
                ClassType.create(LETTUCE_CLIENT_RESOURCES),
                null, recorder.getClientResources()));

        // Register shutdown in correct order: connections → clients → resources
        recorder.cleanup(shutdown);
    }

    /**
     * Creates an application scoped Lettuce synthetic bean with the given type, checkActive guard (a recorder proxy,
     * or {@code null} for an always-active bean), and supplier.
     */
    static SyntheticBeanBuildItem createLettuceBean(String name, DotName implClass, Type beanType,
            Supplier<ActiveResult> checkActive, Supplier<?> supplier) {
        return createLettuceBean(name, implClass, beanType, checkActive, supplier, ApplicationScoped.class);
    }

    /**
     * Creates a Lettuce synthetic bean with the given type, checkActive guard (a recorder proxy, or {@code null}
     * for an always-active bean), supplier and scope.
     */
    static SyntheticBeanBuildItem createLettuceBean(String name, DotName implClass, Type beanType,
            Supplier<ActiveResult> checkActive, Supplier<?> supplier, Class<? extends Annotation> scope) {

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(implClass)
                .addType(beanType)
                .startup()
                .setRuntimeInit()
                .unremovable()
                .supplier(supplier)
                .scope(scope);
        if (checkActive != null) {
            configurator.checkActive(checkActive);
        }

        return qualify(configurator, name).done();
    }

    /**
     * Creates a data source synthetic bean, mirroring the configuration of the Vert.x-backed ones.
     */
    static <T> SyntheticBeanBuildItem createDataSourceBean(String name, Class<T> type,
            Supplier<ActiveResult> checkActive, Supplier<T> supplier) {

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(type)
                .checkActive(checkActive)
                .startup()
                .setRuntimeInit()
                .unremovable()
                .supplier(supplier)
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
