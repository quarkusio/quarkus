package io.quarkus.arc.deployment.configproperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.gizmo.Gizmo;

public class ConfigPropertyBuildItemCandidateUtil {

    /**
     * This method inspects the {@code configClass} bytecode to identify all fields that have a default value set in the class
     * constructor. These fields are removed from the {@link ConfigPropertyBuildItemCandidate} list because we don't want to
     * throw an exception if no config property value was provided for them. There is no bytecode modification performed during
     * this process.
     */
    public static void removePropertiesWithDefaultValue(ClassLoader classLoader, String configClass,
            List<ConfigPropertyBuildItemCandidate> candidates) {
        try (InputStream is = classLoader.getResourceAsStream(configClass.replace('.', '/') + ".class")) {
            ClassReader configClassReader = new ClassReader(is);
            configClassReader.accept(new ConfigClassVisitor(candidates), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(configClass + " class reading failed", e);
        }
    }

    private static class ConfigClassVisitor extends ClassVisitor {

        private List<ConfigPropertyBuildItemCandidate> candidates;

        private ConfigClassVisitor(List<ConfigPropertyBuildItemCandidate> candidates) {
            super(Gizmo.ASM_API_VERSION);
            this.candidates = candidates;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor superMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (access == Modifier.PUBLIC && name.equals("<init>") && descriptor.equals("()V")) {
                return new ConfigClassConstructorVisitor(superMethodVisitor, candidates);
            }
            return superMethodVisitor;
        }
    }

    private static class ConfigClassConstructorVisitor extends MethodVisitor {

        private List<ConfigPropertyBuildItemCandidate> candidates;

        private ConfigClassConstructorVisitor(MethodVisitor superMethodVisitor,
                List<ConfigPropertyBuildItemCandidate> candidates) {
            super(Gizmo.ASM_API_VERSION, superMethodVisitor);
            this.candidates = candidates;
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            // For each instruction in the config class constructor that is setting a field value, we remove that field from
            // the candidates list if the field was part it.
            if (opcode == Opcodes.PUTFIELD) {
                candidates.removeIf(candidate -> candidate.getFieldName().equals(name));
            }
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }
    }
}
