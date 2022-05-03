package io.quarkus.arc.deployment;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;

import java.util.List;
import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.ResourceReferenceProvider;
import io.quarkus.arc.impl.ArcContainerImpl;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.gizmo.Gizmo;

public class ServiceLoadingRemovalProcessor {

    @BuildStep(onlyIf = IsNormal.class)
    public void transformArcContainerImpl(GeneratedComponentProviderBuildItem generatedComponentProviders,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformer) {

        List<String> resourceProviders = ServiceProviderBuildItem
                .allProvidersFromClassPath(ResourceReferenceProvider.class.getName()).providers();
        List<String> componentProviders = generatedComponentProviders.getImplNames();

        bytecodeTransformer.produce(new BytecodeTransformerBuildItem(ArcContainerImpl.class.getName(),
                new BiFunction<>() {
                    @Override
                    public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                        return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                            private static final String CTOR_NAME = "<init>";

                            private static final String ARRAYLIST_BINARY_NAME = "java/util/ArrayList";
                            private static final String LIST_BINARY_NAME = "java/util/List";

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                    String signature, String[] exceptions) {
                                MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);
                                if (name.equals("loadComponentsProviders")) {
                                    return new MethodVisitor(Gizmo.ASM_API_VERSION, original) {
                                        @Override
                                        public void visitCode() {
                                            super.visitCode();
                                            if (componentProviders.isEmpty()) {
                                                visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyList",
                                                        "()Ljava/util/List;",
                                                        false);
                                                visitInsn(Opcodes.ARETURN);
                                            } else {
                                                visitTypeInsn(NEW, ARRAYLIST_BINARY_NAME);
                                                visitInsn(DUP);
                                                visitIntInsn(BIPUSH, componentProviders.size());
                                                visitMethodInsn(INVOKESPECIAL, ARRAYLIST_BINARY_NAME, CTOR_NAME, "(I)V", false);
                                                visitVarInsn(ASTORE, 1);
                                                visitVarInsn(ALOAD, 1);
                                                for (String component : componentProviders) {
                                                    String binaryName = component.replace('.', '/');
                                                    visitTypeInsn(NEW, binaryName);
                                                    visitInsn(DUP);
                                                    visitMethodInsn(INVOKESPECIAL,
                                                            binaryName, CTOR_NAME, "()V", false);
                                                    visitMethodInsn(INVOKEINTERFACE, LIST_BINARY_NAME, "add",
                                                            "(Ljava/lang/Object;)Z", true);
                                                    visitInsn(POP);
                                                    visitVarInsn(ALOAD, 1);
                                                }
                                                visitInsn(ARETURN);
                                            }
                                        }
                                    };
                                } else if (name.equals("loadResourceProviders")) {
                                    return new MethodVisitor(Gizmo.ASM_API_VERSION, original) {
                                        @Override
                                        public void visitCode() {
                                            super.visitCode();
                                            if (resourceProviders.isEmpty()) {
                                                visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyList",
                                                        "()Ljava/util/List;",
                                                        false);
                                                visitInsn(Opcodes.ARETURN);
                                            } else {
                                                visitTypeInsn(NEW, ARRAYLIST_BINARY_NAME);
                                                visitInsn(DUP);
                                                visitIntInsn(BIPUSH, resourceProviders.size());
                                                visitMethodInsn(INVOKESPECIAL, ARRAYLIST_BINARY_NAME, CTOR_NAME, "(I)V", false);
                                                visitVarInsn(ASTORE, 1);
                                                visitVarInsn(ALOAD, 1);
                                                for (String resourceProvider : resourceProviders) {
                                                    String binaryName = resourceProvider.replace('.', '/');
                                                    visitTypeInsn(NEW, binaryName);
                                                    visitInsn(DUP);
                                                    visitMethodInsn(INVOKESPECIAL,
                                                            binaryName, CTOR_NAME, "()V", false);
                                                    visitMethodInsn(INVOKEINTERFACE, LIST_BINARY_NAME, "add",
                                                            "(Ljava/lang/Object;)Z", true);
                                                    visitInsn(POP);
                                                    visitVarInsn(ALOAD, 1);
                                                }
                                                visitInsn(ARETURN);
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
}
