package io.quarkus.kafka.client.deployment;

import java.util.Arrays;
import java.util.Collection;
import java.util.zip.Checksum;

import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.clients.consumer.RoundRobinAssignor;
import org.apache.kafka.clients.consumer.StickyAssignor;
import org.apache.kafka.clients.consumer.internals.PartitionAssignor;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.clients.producer.internals.DefaultPartitioner;
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

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;

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
            FloatDeserializer.class,
    };
    static final String TARGET_JAVA_9_CHECKSUM_FACTORY = "io.quarkus.kafka.client.generated.Target_Java9ChecksumFactory";

    @BuildStep
    public void build(CombinedIndexBuildItem indexBuildItem, BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        Collection<ClassInfo> serializers = indexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(Serializer.class.getName()));
        Collection<ClassInfo> deserializers = indexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(Deserializer.class.getName()));
        Collection<ClassInfo> partitioners = indexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(Partitioner.class.getName()));
        Collection<ClassInfo> partitionAssignors = indexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(PartitionAssignor.class.getName()));

        for (Class i : BUILT_INS) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, i.getName()));
        }

        for (Collection<ClassInfo> list : Arrays.asList(serializers, deserializers, partitioners, partitionAssignors)) {
            for (ClassInfo s : list) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, s.toString()));
            }
        }

        // built in partitioner and partition assignors
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, DefaultPartitioner.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, RangeAssignor.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, RoundRobinAssignor.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, StickyAssignor.class.getName()));

    }

    /**
     * Generate a class which replaces the usage of {@code MethodHandle} in {@code Java9ChecksumFactory} with a plain
     * constructor invocation when run under GraalVM. This is necessary because the native image generator does not
     * support method handles.
     *
     * @return the generated class
     */
    @BuildStep
    public GeneratedClassBuildItem replaceJava9Code() {
        // make our own class output to ensure that our step is run.
        byte[][] holder = new byte[1][];
        ClassOutput classOutput = new ClassOutput() {
            public void write(final String name, final byte[] data) {
                holder[0] = data;
            }
        };
        try (ClassCreator cc = ClassCreator.builder().className(TARGET_JAVA_9_CHECKSUM_FACTORY)
                .classOutput(classOutput).setFinal(true).superClass(Object.class).build()) {

            cc.addAnnotation("com/oracle/svm/core/annotate/TargetClass").addValue("className",
                    "org.apache.kafka.common.utils.Crc32C$Java9ChecksumFactory");
            cc.addAnnotation("com/oracle/svm/core/annotate/Substitute");

            try (MethodCreator mc = cc.getMethodCreator("create", Checksum.class)) {
                mc.addAnnotation("com/oracle/svm/core/annotate/Substitute");
                mc.returnValue(mc.newInstance(MethodDescriptor.ofConstructor("java.util.zip.CRC32C")));
            }
        }
        return new GeneratedClassBuildItem(false, TARGET_JAVA_9_CHECKSUM_FACTORY, holder[0]);
    }
}
