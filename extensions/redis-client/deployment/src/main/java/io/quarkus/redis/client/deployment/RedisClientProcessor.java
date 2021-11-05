package io.quarkus.redis.client.deployment;

import static io.quarkus.arc.processor.BuiltinScope.SINGLETON;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.client.RedisHostsProvider;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.quarkus.redis.client.runtime.MutinyRedis;
import io.quarkus.redis.client.runtime.MutinyRedisAPI;
import io.quarkus.redis.client.runtime.RedisClientRecorder;
import io.quarkus.redis.client.runtime.RedisClientUtil;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.impl.types.BulkType;

public class RedisClientProcessor {

    private static final DotName REDIS_CLIENT_ANNOTATION = DotName.createSimple(RedisClientName.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.REDIS_CLIENT);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(Feature.REDIS_CLIENT.getName());
    }

    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .setUnremovable()
                .addBeanClass(RedisHostsProvider.class)
                .build();
    }

    @BuildStep
    List<AdditionalBeanBuildItem> registerRedisBeans() {
        return Arrays.asList(
                AdditionalBeanBuildItem
                        .builder()
                        .addBeanClass("io.quarkus.redis.client.runtime.RedisClientsProducer")
                        .setDefaultScope(SINGLETON.getName())
                        .setUnremovable()
                        .build(),
                AdditionalBeanBuildItem
                        .builder()
                        .addBeanClass(RedisClientName.class)
                        .build());
    }

    @BuildStep
    HealthBuildItem addHealthCheck(RedisBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.redis.client.runtime.health.RedisHealthCheck", buildTimeConfig.healthEnabled);
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
        // RedisClusterConnections is referenced from RedisClusterClient. Thus, we need to runtime-init
        // that too.
        producer.produce(new RuntimeInitializedClassBuildItem("io.vertx.redis.client.impl.RedisClusterClient"));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void produceRedisClient(RedisClientRecorder recorder, BeanArchiveIndexBuildItem indexBuildItem,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            VertxBuildItem vertxBuildItem) {
        Set<String> clientNames = new HashSet<>();
        clientNames.add(RedisClientUtil.DEFAULT_CLIENT);

        IndexView indexView = indexBuildItem.getIndex();
        Collection<AnnotationInstance> clientAnnotations = indexView.getAnnotations(REDIS_CLIENT_ANNOTATION);
        for (AnnotationInstance annotation : clientAnnotations) {
            clientNames.add(annotation.value().asString());
        }

        for (String clientName : clientNames) {
            syntheticBeans.produce(createRedisClientSyntheticBean(recorder, clientName));
            syntheticBeans.produce(createRedisReactiveClientSyntheticBean(recorder, clientName));
            syntheticBeans.produce(createMutinyRedisAPISyntheticBean(recorder, clientName));
            syntheticBeans.produce(createMutinyRedisSyntheticBean(recorder, clientName));
            syntheticBeans.produce(createRedisSyntheticBean(recorder, clientName));
            syntheticBeans.produce(createRedisAPISyntheticBean(recorder, clientName));
        }
    }

    private SyntheticBeanBuildItem createRedisClientSyntheticBean(RedisClientRecorder recorder, String clientName) {
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(RedisClient.class)
                .scope(ApplicationScoped.class)
                .supplier(recorder.redisClientSupplier(clientName))
                .setRuntimeInit();

        return applyCommonBeanConfig(clientName, configurator);
    }

    private SyntheticBeanBuildItem createRedisReactiveClientSyntheticBean(RedisClientRecorder recorder, String clientName) {
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(ReactiveRedisClient.class)
                .scope(ApplicationScoped.class)
                .supplier(recorder.reactiveRedisClientSupplier(clientName))
                .setRuntimeInit();

        return applyCommonBeanConfig(clientName, configurator);
    }

    private SyntheticBeanBuildItem createMutinyRedisSyntheticBean(RedisClientRecorder recorder, String clientName) {
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(MutinyRedis.class)
                .scope(ApplicationScoped.class)
                .supplier(recorder.mutinyRedisSupplier(clientName))
                .setRuntimeInit();

        return applyCommonBeanConfig(clientName, configurator);
    }

    private SyntheticBeanBuildItem createMutinyRedisAPISyntheticBean(RedisClientRecorder recorder, String clientName) {
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(MutinyRedisAPI.class)
                .scope(ApplicationScoped.class)
                .supplier(recorder.mutinyRedisAPISupplier(clientName))
                .setRuntimeInit();

        return applyCommonBeanConfig(clientName, configurator);
    }

    private SyntheticBeanBuildItem createRedisSyntheticBean(RedisClientRecorder recorder, String clientName) {
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(Redis.class)
                .scope(ApplicationScoped.class)
                .supplier(recorder.redisSupplier(clientName))
                .setRuntimeInit();

        return applyCommonBeanConfig(clientName, configurator);
    }

    private SyntheticBeanBuildItem createRedisAPISyntheticBean(RedisClientRecorder recorder, String clientName) {
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(RedisAPI.class)
                .scope(ApplicationScoped.class)
                .supplier(recorder.redisAPISupplier(clientName))
                .setRuntimeInit();

        return applyCommonBeanConfig(clientName, configurator);
    }

    private SyntheticBeanBuildItem applyCommonBeanConfig(String clientName,
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator) {

        configurator.unremovable();

        if (RedisClientUtil.isDefault(clientName)) {
            configurator.addQualifier(Default.class);
        } else {
            configurator.addQualifier().annotation(REDIS_CLIENT_ANNOTATION).addValue("value", clientName).done();
        }
        return configurator.done();
    }
}
