package io.quarkus.hibernate.orm.panache.deployment;

import java.util.Map;
import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PanacheFieldAccessEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private Map<String, EntityModel> entities;

    public PanacheFieldAccessEnhancer(Map<String, EntityModel> entities) {
        this.entities = entities;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new FieldAccessClassVisitor(className, outputClassVisitor, entities);
    }

    static class FieldAccessClassVisitor extends ClassVisitor {

        private Map<String, EntityModel> entities;
        private String classBinaryName;

        public FieldAccessClassVisitor(String className, ClassVisitor outputClassVisitor, Map<String, EntityModel> entities) {
            super(Opcodes.ASM7, outputClassVisitor);
            this.entities = entities;
            this.classBinaryName = className.replace('.', '/');
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
                String[] exceptions) {
            MethodVisitor superVisitor = super.visitMethod(access, methodName, descriptor, signature, exceptions);
            return new PanacheFieldAccessMethodVisitor(superVisitor, classBinaryName, methodName, descriptor, entities);
        }
    }
}
