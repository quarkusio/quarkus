package io.quarkus.kafka.client.deployment;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import javax.security.auth.spi.LoginModule;

import org.apache.kafka.clients.consumer.ConsumerPartitionAssignor;
import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.clients.consumer.RoundRobinAssignor;
import org.apache.kafka.clients.consumer.StickyAssignor;
import org.apache.kafka.clients.consumer.internals.PartitionAssignor;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.clients.producer.internals.DefaultPartitioner;
import org.apache.kafka.common.security.authenticator.AbstractLogin;
import org.apache.kafka.common.security.authenticator.DefaultLogin;
import org.apache.kafka.common.security.authenticator.SaslClientCallbackHandler;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.ByteBufferDeserializer;
import org.apache.kafka.common.serialization.ByteBufferSerializer;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.DoubleDeserializer;
import org.apache.kafka.common.serialization.DoubleSerializer;
import org.apache.kafka.common.serialization.FloatDeserializer;
import org.apache.kafka.common.serialization.FloatSerializer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.ShortDeserializer;
import org.apache.kafka.common.serialization.ShortSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.kafka.client.runtime.KafkaRuntimeConfigProducer;
import io.quarkus.kafka.client.serialization.JsonbDeserializer;
import io.quarkus.kafka.client.serialization.JsonbSerializer;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

public class KafkaProcessor {

    static final Class[] BUILT_INS = {
            //serializers
            ShortSerializer.class,
            DoubleSerializer.class,
            LongSerializer.class,
            BytesSerializer.class,
            ByteArraySerializer.class,
            IntegerSerializer.class,
            ByteBufferSerializer.class,
            StringSerializer.class,
            FloatSerializer.class,

            //deserializers
            ShortDeserializer.class,
            DoubleDeserializer.class,
            LongDeserializer.class,
            BytesDeserializer.class,
            ByteArrayDeserializer.class,
            IntegerDeserializer.class,
            ByteBufferDeserializer.class,
            StringDeserializer.class,
            FloatDeserializer.class
    };

    @BuildStep
    void contributeClassesToIndex(BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClasses,
            BuildProducer<IndexDependencyBuildItem> indexDependency) {
        // This is needed for SASL authentication

        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(
                LoginModule.class.getName(),
                javax.security.auth.Subject.class.getName(),
                javax.security.auth.login.AppConfigurationEntry.class.getName(),
                javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.class.getName()));

        indexDependency.produce(new IndexDependencyBuildItem("org.apache.kafka", "kafka-clients"));
    }

    @BuildStep
    public void build(CombinedIndexBuildItem indexBuildItem, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            Capabilities capabilities) {
        final Set<DotName> toRegister = new HashSet<>();

        collectImplementors(toRegister, indexBuildItem, Serializer.class);
        collectImplementors(toRegister, indexBuildItem, Deserializer.class);
        collectImplementors(toRegister, indexBuildItem, Partitioner.class);
        // PartitionAssignor is now deprecated, replaced by ConsumerPartitionAssignor
        collectImplementors(toRegister, indexBuildItem, PartitionAssignor.class);
        collectImplementors(toRegister, indexBuildItem, ConsumerPartitionAssignor.class);

        for (Class<?> i : BUILT_INS) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, i.getName()));
            collectSubclasses(toRegister, indexBuildItem, i);
        }
        if (capabilities.isCapabilityPresent(Capabilities.JSONB)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, JsonbSerializer.class, JsonbDeserializer.class));
            collectSubclasses(toRegister, indexBuildItem, JsonbSerializer.class);
            collectSubclasses(toRegister, indexBuildItem, JsonbDeserializer.class);
        }
        if (capabilities.isCapabilityPresent(Capabilities.JACKSON)) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(false, false, ObjectMapperSerializer.class, ObjectMapperDeserializer.class));
            collectSubclasses(toRegister, indexBuildItem, ObjectMapperSerializer.class);
            collectSubclasses(toRegister, indexBuildItem, ObjectMapperDeserializer.class);
        }

        for (DotName s : toRegister) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, s.toString()));
        }

        // built in partitioner and partition assignors
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, DefaultPartitioner.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, RangeAssignor.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, RoundRobinAssignor.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, StickyAssignor.class.getName()));

        // classes needed to perform reflection on DirectByteBuffer - only really needed for Java 8
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "java.nio.DirectByteBuffer"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "sun.misc.Cleaner"));
    }

    @BuildStep
    public AdditionalBeanBuildItem runtimeConfig() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(KafkaRuntimeConfigProducer.class)
                .setUnremovable()
                .build();
    }

    @BuildStep
    public void withSasl(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, AbstractLogin.DefaultLoginCallbackHandler.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, SaslClientCallbackHandler.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, DefaultLogin.class));

        final Type loginModuleType = Type
                .create(DotName.createSimple(LoginModule.class.getName()), Kind.CLASS);

        reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(loginModuleType));
    }

    private static void collectImplementors(Set<DotName> set, CombinedIndexBuildItem indexBuildItem, Class<?> cls) {
        collectClassNames(set, indexBuildItem.getIndex().getAllKnownImplementors(DotName.createSimple(cls.getName())));
    }

    private static void collectSubclasses(Set<DotName> set, CombinedIndexBuildItem indexBuildItem, Class<?> cls) {
        collectClassNames(set, indexBuildItem.getIndex().getAllKnownSubclasses(DotName.createSimple(cls.getName())));
    }

    private static void collectClassNames(Set<DotName> set, Collection<ClassInfo> classInfos) {
        classInfos.forEach(new Consumer<ClassInfo>() {
            @Override
            public void accept(ClassInfo c) {
                set.add(c.name());
            }
        });
    }

    @BuildStep
    HealthBuildItem addHealthCheck(KafkaBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.kafka.client.health.KafkaHealthCheck",
                buildTimeConfig.healthEnabled);
    }
}
