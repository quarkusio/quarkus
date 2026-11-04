package org.acme.transform.extension.deployment;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.smallrye.classfile.ClassFile;

public class TransformProcessor {

    @BuildStep
    JniRuntimeAccessBuildItem registerJniAccess() {
        return new JniRuntimeAccessBuildItem(true, false, false, "org.acme.libjni.JniTarget");
    }

    @BuildStep
    NativeImageSecurityProviderBuildItem registerSecurityProvider() {
        return new NativeImageSecurityProviderBuildItem("org.acme.nativeconfig.NativeConfigSecurityProvider");
    }

    /**
     * A class generated only for native-image, like a GraalVM Feature, referencing a class nothing else uses.
     */
    @BuildStep
    GeneratedNativeImageClassBuildItem generateNativeImageClass() {
        final String name = "org.acme.transform.extension.NativeImageOnlyClass";
        final byte[] bytecode = ClassFile.of().build(ClassDesc.of(name), classBuilder -> classBuilder
                .withFlags(ClassFile.ACC_PUBLIC)
                .withMethodBody("refs", MethodTypeDesc.of(ConstantDescs.CD_void),
                        ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC, code -> code
                                .loadConstant(ClassDesc.of("org.acme.nativeconfig.NativeImageClassReference"))
                                .pop()
                                .return_()));
        return new GeneratedNativeImageClassBuildItem(name.replace('.', '/'), bytecode);
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
