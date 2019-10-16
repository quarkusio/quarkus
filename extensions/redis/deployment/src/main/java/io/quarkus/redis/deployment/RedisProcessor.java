package io.quarkus.redis.deployment;

import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.lettuce.core.EpollProvider;
import io.lettuce.core.KqueueProvider;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.dynamic.Commands;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.redis.runtime.QuarkusRedisCommandFactory;
import io.quarkus.redis.runtime.RedisConfig;
import io.quarkus.redis.runtime.RedisRecorder;

class RedisProcessor {
    private static final DotName COMMANDS_DOT_NAME = DotName.createSimple(Commands.class.getName());
    private static final String[] DYNAMIC_SUPPORTED_SERIALIZABLE_TYPES = {
            GenericArrayType.class.getName(),
            ParameterizedType.class.getName(),
            TypeVariable.class.getName(),
            WildcardType.class.getName()
    };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.REDIS);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.REDIS);
    }

    @BuildStep
    AdditionalBeanBuildItem registerFactoryBean() {
        return AdditionalBeanBuildItem.unremovableOf(QuarkusRedisCommandFactory.class);
    }

    @BuildStep
    List<RuntimeInitializedClassBuildItem> initializeAtRuntime() {
        return Arrays.asList(
                new RuntimeInitializedClassBuildItem(KqueueProvider.class.getName()),
                new RuntimeInitializedClassBuildItem(EpollProvider.class.getName()),
                new RuntimeInitializedClassBuildItem("io.lettuce.core.EpollProvider$AvailableEpollResources"),
                new RuntimeInitializedClassBuildItem("io.lettuce.core.KqueueProvider$AvailableKqueueResources"),
                new RuntimeInitializedClassBuildItem(io.lettuce.core.AbstractRedisClient.class.getName()));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void initializeRedisConnectionFactory(RedisRecorder redisRecorder, RedisConfig config,
            ShutdownContextBuildItem shutdownContextBuildItem) {
        redisRecorder.configureTheClient(config, shutdownContextBuildItem);
    }

    @BuildStep
    List<NativeImageProxyDefinitionBuildItem> registerSubstrateProxy() {
        List<NativeImageProxyDefinitionBuildItem> proxies = new ArrayList<>();
        proxies.add(new NativeImageProxyDefinitionBuildItem(RedisCommands.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(RedisSentinelCommands.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(RedisAdvancedClusterCommands.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(StatefulConnection.class.getName(),
                StatefulRedisClusterConnection.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(StatefulConnection.class.getName(),
                StatefulRedisSentinelConnection.class.getName()));
        proxies.add(new NativeImageProxyDefinitionBuildItem(StatefulConnection.class.getName(),
                StatefulRedisConnection.class.getName()));

        for (String type : DYNAMIC_SUPPORTED_SERIALIZABLE_TYPES) {
            proxies.add(new NativeImageProxyDefinitionBuildItem(
                    type,
                    "io.lettuce.core.dynamic.support.TypeWrapper$SerializableTypeProxy",
                    Serializable.class.getName()));
        }

        return proxies;
    }

    @BuildStep
    void scanForRedisCommands(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy, BuildProducer<GeneratedBeanBuildItem> beanProducer,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies) {
        IndexView indexView = combinedIndexBuildItem.getIndex();
        RedisCommandsClassGenerator generator = new RedisCommandsClassGenerator();

        List<ClassInfo> redisCommandInterfaces = new ArrayList<>(getKnownCommandsSubInterfaces(indexView));

        for (ClassInfo classInfo : redisCommandInterfaces) {
            // generate redis command interface implementation
            ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(beanProducer);
            generator.generate(classOutput, classInfo);

            // generate proxy
            proxies.produce(new NativeImageProxyDefinitionBuildItem(classInfo.name().toString()));

            // register the whole interface hierarchy for reflection
            reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(Type.create(classInfo.name(), Type.Kind.CLASS)));
        }
    }

    // This is inefficient, let's use this until https://github.com/wildfly/jandex/issues/65 is solved
    private static List<ClassInfo> getKnownCommandsSubInterfaces(IndexView index) {
        List<ClassInfo> knownCommandsSubInterfaces = new ArrayList<>();
        Collection<ClassInfo> knownClasses = index.getKnownClasses();
        for (ClassInfo clazz : knownClasses) {
            if (!Modifier.isInterface(clazz.flags())) {
                continue;
            }

            List<DotName> interfaceNames = clazz.interfaceNames();
            if (interfaceNames.contains(COMMANDS_DOT_NAME)) {
                knownCommandsSubInterfaces.add(clazz);
            }
        }

        return knownCommandsSubInterfaces;
    }
}
