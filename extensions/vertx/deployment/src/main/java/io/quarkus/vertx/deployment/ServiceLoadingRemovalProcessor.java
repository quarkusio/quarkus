package io.quarkus.vertx.deployment;

import static org.objectweb.asm.Opcodes.ARETURN;

import java.util.List;
import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.gizmo.Gizmo;
import io.vertx.core.impl.VerticleManager;
import io.vertx.core.impl.VertxBuilder;

public class ServiceLoadingRemovalProcessor {

    //TODO: this simply returns empty lists because we don't currently register the implementations with ServiceProviderBuildItem

    @BuildStep(onlyIf = IsNormal.class)
    public void transformSmallRyeConfigBuilder(List<ServiceProviderBuildItem> servicesItems,
            BuildProducer<BytecodeTransformerBuildItem> producer) {
        producer.produce(new BytecodeTransformerBuildItem(VertxBuilder.class.getName(),
                new BiFunction<>() {
                    @Override
                    public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                        return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                    String signature, String[] exceptions) {
                                MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);
                                if (name.equals("loadProviders")) {
                                    return new MethodVisitor(Gizmo.ASM_API_VERSION, original) {
                                        @Override
                                        public void visitCode() {
                                            super.visitCode();
                                            visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyList",
                                                    "()Ljava/util/List;",
                                                    false);
                                            visitInsn(Opcodes.ARETURN);
                                        }
                                    };
                                }

                                return original;
                            }
                        };
                    }
                }));

        producer.produce(new BytecodeTransformerBuildItem(VerticleManager.class.getName(),
                new BiFunction<>() {
                    @Override
                    public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                        return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                    String signature, String[] exceptions) {
                                MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);
                                if (name.equals("loadFactories")) {
                                    return new MethodVisitor(Gizmo.ASM_API_VERSION, original) {
                                        @Override
                                        public void visitCode() {
                                            super.visitCode();
                                            visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyList",
                                                    "()Ljava/util/List;",
                                                    false);
                                            visitInsn(Opcodes.ARETURN);
                                        }
                                    };
                                }

                                return original;
                            }
                        };
                    }
                }));
    }
}
