package org.acme.transform.extension.deployment;

import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;

public class TransformProcessor {

    @BuildStep
    JniRuntimeAccessBuildItem registerJniAccess() {
        return new JniRuntimeAccessBuildItem(true, false, false, "org.acme.libjni.JniTarget");
    }

    @BuildStep
    BytecodeTransformerBuildItem transformClass() {
        return new BytecodeTransformerBuildItem("org.acme.transform.TransformableClass",
                new BiFunction<String, ClassVisitor, ClassVisitor>() {
                    @Override
                    public ClassVisitor apply(String className, ClassVisitor cv) {
                        return new ClassVisitor(Opcodes.ASM9, cv) {
                            @Override
                            public void visit(int version, int access, String name, String signature,
                                    String superName, String[] interfaces) {
                                String[] newInterfaces;
                                if (interfaces == null) {
                                    newInterfaces = new String[] { "org/acme/transform/TransformAddedRef" };
                                } else {
                                    newInterfaces = new String[interfaces.length + 1];
                                    System.arraycopy(interfaces, 0, newInterfaces, 0, interfaces.length);
                                    newInterfaces[interfaces.length] = "org/acme/transform/TransformAddedRef";
                                }
                                super.visit(version, access, name, signature, superName, newInterfaces);
                            }
                        };
                    }
                });
    }
}
