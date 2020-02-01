package io.quarkus.panache.common.deployment;

import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import io.quarkus.gizmo.Gizmo;

public class PanacheFieldAccessEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private MetamodelInfo<?> modelInfo;

    public PanacheFieldAccessEnhancer(MetamodelInfo<?> modelInfo) {
        this.modelInfo = modelInfo;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new FieldAccessClassVisitor(className, outputClassVisitor, modelInfo);
    }

    static class FieldAccessClassVisitor extends ClassVisitor {

        private String classBinaryName;
        private MetamodelInfo<?> modelInfo;

        public FieldAccessClassVisitor(String className, ClassVisitor outputClassVisitor, MetamodelInfo<?> modelInfo) {
            super(Gizmo.ASM_API_VERSION, outputClassVisitor);
            this.modelInfo = modelInfo;
            this.classBinaryName = className.replace('.', '/');
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
                String[] exceptions) {
            MethodVisitor superVisitor = super.visitMethod(access, methodName, descriptor, signature, exceptions);
            return new PanacheFieldAccessMethodVisitor(superVisitor, classBinaryName, methodName, descriptor, modelInfo);
        }
    }
}
