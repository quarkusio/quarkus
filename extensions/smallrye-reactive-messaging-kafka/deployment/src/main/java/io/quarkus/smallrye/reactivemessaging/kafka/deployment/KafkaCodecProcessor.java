package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.smallrye.reactivemessaging.kafka.KafkaCodecDependencyRemovalLogger;
import io.vertx.kafka.client.serialization.BufferDeserializer;
import io.vertx.kafka.client.serialization.BufferSerializer;
import io.vertx.kafka.client.serialization.JsonArrayDeserializer;
import io.vertx.kafka.client.serialization.JsonArraySerializer;
import io.vertx.kafka.client.serialization.JsonObjectDeserializer;
import io.vertx.kafka.client.serialization.JsonObjectSerializer;

public class KafkaCodecProcessor {

    static final Class<?>[] VERTX_KAFKA_CLIENT_SERDES = {
            JsonObjectSerializer.class,
            BufferSerializer.class,
            JsonArraySerializer.class,

            JsonObjectDeserializer.class,
            BufferDeserializer.class,
            JsonArrayDeserializer.class,
    };

    @BuildStep
    public void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        for (Class<?> s : VERTX_KAFKA_CLIENT_SERDES) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, s.getName()));
        }
    }

    @BuildStep
    public void deprecateVertxProvidedSerde(BuildProducer<BytecodeTransformerBuildItem> producer) {
        for (Class<?> vertxSerdeClass : VERTX_KAFKA_CLIENT_SERDES) {
            producer.produce(new BytecodeTransformerBuildItem(vertxSerdeClass.getName(),
                    KafkaCodecDeprecateClassVisitor::new));
        }
    }

    private static class KafkaCodecDeprecateClassVisitor extends ClassVisitor {

        private final String fqcn;

        protected KafkaCodecDeprecateClassVisitor(String fqcn, ClassVisitor classVisitor) {
            super(Gizmo.ASM_API_VERSION, classVisitor);
            this.fqcn = fqcn;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals("<init>")) {
                return new MethodVisitor(Gizmo.ASM_API_VERSION, methodVisitor) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        visitLdcInsn(fqcn); // load fqcn as constant
                        visitMethodInsn(Opcodes.INVOKESTATIC,
                                KafkaCodecDependencyRemovalLogger.class.getName().replace(".", "/"), "logDependencyRemoval",
                                "(Ljava/lang/String;)V", false);
                    }
                };
            } else {
                return methodVisitor;
            }
        }
    }
}
