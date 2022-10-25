package io.quarkus.redis.client.deployment;

import static io.quarkus.redis.runtime.client.config.RedisConfig.DEFAULT_CLIENT_NAME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.client.RedisHostsProvider;
import io.quarkus.redis.client.RedisOptionsCustomizer;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.quarkus.redis.runtime.client.RedisClientRecorder;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
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
    public void init(RedisClientRecorder recorder,
            BeanArchiveIndexBuildItem indexBuildItem,
            BeanDiscoveryFinishedBuildItem beans,
            ShutdownContextBuildItem shutdown,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            VertxBuildItem vertxBuildItem) {

        // Collect the used redis clients, the unused clients will not be instantiated.
        Set<String> names = new HashSet<>();
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

        // Inject the creation of the client when the application starts.
        recorder.initialize(vertxBuildItem.getVertx(), names);

        // Create the supplier and define the beans.
        for (String name : names) {
            // Redis objects
            syntheticBeans.produce(configureAndCreateSyntheticBean(name, io.vertx.mutiny.redis.client.Redis.class,
                    recorder.getRedisClient(name)));
            syntheticBeans.produce(configureAndCreateSyntheticBean(name, io.vertx.redis.client.Redis.class,
                    recorder.getBareRedisClient(name)));

            // Redis API objects
            syntheticBeans.produce(configureAndCreateSyntheticBean(name, io.vertx.mutiny.redis.client.RedisAPI.class,
                    recorder.getRedisAPI(name)));
            syntheticBeans.produce(configureAndCreateSyntheticBean(name, io.vertx.redis.client.RedisAPI.class,
                    recorder.getBareRedisAPI(name)));

            // Legacy clients
            syntheticBeans
                    .produce(configureAndCreateSyntheticBean(name, RedisClient.class, recorder.getLegacyRedisClient(name)));
            syntheticBeans.produce(configureAndCreateSyntheticBean(name, ReactiveRedisClient.class,
                    recorder.getLegacyReactiveRedisClient(name)));
        }

        recorder.cleanup(shutdown);
    }

    static <T> SyntheticBeanBuildItem configureAndCreateSyntheticBean(String name,
            Class<T> type,
            Supplier<T> supplier) {
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(type)
                .supplier(supplier)
                .scope(ApplicationScoped.class)
                .unremovable()
                .setRuntimeInit();

        if (DEFAULT_CLIENT_NAME.equalsIgnoreCase(name)) {
            configurator.addQualifier(Default.class);
        } else {
            configurator.addQualifier().annotation(REDIS_CLIENT_ANNOTATION).addValue("value", name).done();
        }

        return configurator.done();
    }

    @BuildStep
    HealthBuildItem addHealthCheck(RedisBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.redis.runtime.client.health.RedisHealthCheck",
                buildTimeConfig.healthEnabled);
    }
}
