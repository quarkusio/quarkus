package io.quarkus.resteasy.reactive.common.deployment;

import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.NEW;

import java.util.function.BiFunction;

import javax.ws.rs.ext.RuntimeDelegate;

import org.jboss.resteasy.reactive.common.jaxrs.RuntimeDelegateImpl;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.gizmo.Gizmo;

public class ServiceLoadingRemovalProcessor {

    @BuildStep(onlyIf = IsNormal.class)
    public void transformArcContainerImpl(Capabilities capabilities,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformer) {

        boolean isServerPresent = capabilities.isPresent(Capability.RESTEASY_REACTIVE);
        boolean isClientPresent = capabilities.isPresent(Capability.REST_CLIENT);

        if (!(isServerPresent || isClientPresent)) {
            return;
        }
        bytecodeTransformer.produce(new BytecodeTransformerBuildItem(RuntimeDelegateImpl.class.getName(),
                new BiFunction<>() {
                    @Override
                    public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                        return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                            private static final String CTOR_NAME = "<init>";

                            private static final String SERVER_FACTORY_BINARY_NAME = "org/jboss/resteasy/reactive/server/core/ServerResponseBuilderFactory";
                            private static final String CLIENT_FACTORY_BINARY_NAME = "io/quarkus/jaxrs/client/reactive/runtime/ClientResponseBuilderFactory";

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                    String signature, String[] exceptions) {
                                MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);
                                if (name.equals("loadAndDetermineResponseBuilderFactory")) {
                                    return new MethodVisitor(Gizmo.ASM_API_VERSION, original) {
                                        @Override
                                        public void visitCode() {
                                            super.visitCode();
                                            if (isServerPresent) {
                                                visitTypeInsn(NEW, SERVER_FACTORY_BINARY_NAME);
                                                visitInsn(DUP);
                                                visitMethodInsn(Opcodes.INVOKESPECIAL, SERVER_FACTORY_BINARY_NAME,
                                                        CTOR_NAME, "()V",
                                                        false);
                                                visitInsn(Opcodes.ARETURN);
                                            } else if (isClientPresent) {
                                                visitTypeInsn(NEW, CLIENT_FACTORY_BINARY_NAME);
                                                visitInsn(DUP);
                                                visitMethodInsn(Opcodes.INVOKESPECIAL, CLIENT_FACTORY_BINARY_NAME,
                                                        CTOR_NAME, "()V",
                                                        false);
                                                visitInsn(Opcodes.ARETURN);
                                            }
                                        }
                                    };
                                }
                                return original;
                            }
                        };
                    }
                }));
    }

    @BuildStep(onlyIf = IsNormal.class)
    public void transformRuntimeDelegate(Capabilities capabilities,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformer) {
        if (!capabilities.isPresent(Capability.RESTEASY_REACTIVE)) {
            return;
        }
        bytecodeTransformer.produce(new BytecodeTransformerBuildItem(RuntimeDelegate.class.getName(),
                new BiFunction<>() {
                    @Override
                    public ClassVisitor apply(String s, ClassVisitor classVisitor) {

                        return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                            private static final String CTOR_NAME = "<init>";

                            private static final String RUNTIME_DELEGATE_IMPL_BINARY_NAME = "org/jboss/resteasy/reactive/common/jaxrs/RuntimeDelegateImpl";

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                    String signature, String[] exceptions) {
                                MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);
                                if (name.equals("findDelegate")) {
                                    return new MethodVisitor(Gizmo.ASM_API_VERSION, original) {
                                        @Override
                                        public void visitCode() {
                                            super.visitCode();
                                            visitTypeInsn(NEW, RUNTIME_DELEGATE_IMPL_BINARY_NAME);
                                            visitInsn(DUP);
                                            visitMethodInsn(Opcodes.INVOKESPECIAL, RUNTIME_DELEGATE_IMPL_BINARY_NAME,
                                                    CTOR_NAME, "()V",
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
