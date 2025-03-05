package io.quarkus.redis.deployment.client;

import static io.quarkus.redis.deployment.client.RedisClientProcessor.REDIS_CLIENT_ANNOTATION;
import static io.quarkus.redis.deployment.client.RedisClientProcessor.configureAndCreateSyntheticBean;
import static io.quarkus.redis.deployment.client.RedisClientProcessor.configuredClientNames;
import static io.quarkus.redis.runtime.client.config.RedisConfig.DEFAULT_CLIENT_NAME;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.codecs.Codec;
import io.quarkus.redis.runtime.client.RedisClientRecorder;
import io.quarkus.tls.deployment.spi.TlsRegistryBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;

public class RedisDatasourceProcessor {

    private static final List<DotName> SUPPORTED_INJECTION_TYPE = List.of(
            DotName.createSimple(RedisDataSource.class.getName()),
            DotName.createSimple(ReactiveRedisDataSource.class.getName()));

    @BuildStep
    public void detectUsage(BuildProducer<RequestedRedisClientBuildItem> request,
            RedisBuildTimeConfig buildTimeConfig,
            BeanArchiveIndexBuildItem indexBuildItem,
            BeanDiscoveryFinishedBuildItem beans) {
        // Collect the used redis datasource, the unused clients will not be instantiated.
        Set<String> names = new HashSet<>();
        IndexView indexView = indexBuildItem.getIndex();
        Collection<AnnotationInstance> clientAnnotations = indexView.getAnnotations(REDIS_CLIENT_ANNOTATION);
        for (AnnotationInstance annotation : clientAnnotations) {
            names.add(annotation.value().asString());
        }

        // Check if the application use the default Redis datasource.
        beans.getInjectionPoints().stream().filter(InjectionPointInfo::hasDefaultedQualifier)
                .filter(i -> SUPPORTED_INJECTION_TYPE.contains(i.getRequiredType().name()))
                .findAny()
                .ifPresent(x -> names.add(DEFAULT_CLIENT_NAME));

        beans.getInjectionPoints().stream()
                .filter(i -> SUPPORTED_INJECTION_TYPE.contains(i.getRequiredType().name()))
                .filter(InjectionPointInfo::isProgrammaticLookup)
                .findAny()
                .ifPresent(x -> names.addAll(configuredClientNames(buildTimeConfig, ConfigProvider.getConfig())));

        for (String name : names) {
            request.produce(new RequestedRedisClientBuildItem(name));
        }
    }

    @BuildStep
    public void makeCodecsUnremovable(CombinedIndexBuildItem index, BuildProducer<AdditionalBeanBuildItem> producer) {
        producer.produce(AdditionalBeanBuildItem.unremovableOf(Codec.class));

        for (ClassInfo implementor : index.getIndex().getAllKnownImplementors(Codec.class.getName())) {
            producer.produce(AdditionalBeanBuildItem.unremovableOf(implementor.name().toString()));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void init(RedisClientRecorder recorder,
            List<RequestedRedisClientBuildItem> clients,
            ShutdownContextBuildItem shutdown,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            VertxBuildItem vertxBuildItem,
            TlsRegistryBuildItem tlsRegistryBuildItem) {

        if (clients.isEmpty()) {
            return;
        }
        Set<String> names = new HashSet<>();
        for (RequestedRedisClientBuildItem client : clients) {
            names.add(client.name);
        }
        // Inject the creation of the client when the application starts.
        recorder.initialize(vertxBuildItem.getVertx(), names, tlsRegistryBuildItem.registry());

        // Create the supplier and define the beans.
        for (String name : names) {
            // Data sources
            syntheticBeans.produce(configureAndCreateSyntheticBean(name, RedisDataSource.class,
                    recorder.getBlockingDataSource(name)));
            syntheticBeans.produce(configureAndCreateSyntheticBean(name, ReactiveRedisDataSource.class,
                    recorder.getReactiveDataSource(name)));
        }

        recorder.cleanup(shutdown);
    }
}
