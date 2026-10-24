package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class JarTreeShakerTest {

    private static final Handle LOAD_CLASS = new Handle(Opcodes.H_INVOKESTATIC, "io/quarkus/core/Bootstraps", "loadClass",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Class;",
            false);

    @Test
    void constantDynamicReferencesAreCollected() {
        ConstantDynamic condy = new ConstantDynamic("captured", "Ljava/lang/Class;", LOAD_CLASS,
                "Lcom/example/Captured;");
        Set<String> refs = JarTreeShaker.extractReferencesFromBytecode(classWithLdc(condy));

        assertThat(refs).contains("io.quarkus.core.Bootstraps", "com.example.Captured");
    }

    @Test
    void nestedConstantDynamicReferencesAreCollected() {
        ConstantDynamic inner = new ConstantDynamic("inner", "Ljava/lang/Class;", LOAD_CLASS, "[Lcom/example/Element;");
        Handle outerBootstrap = new Handle(Opcodes.H_INVOKESTATIC, "com/example/Bootstrap", "wrap",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;)Lcom/example/Wrapper;",
                false);
        ConstantDynamic outer = new ConstantDynamic("outer", "Lcom/example/Wrapper;", outerBootstrap, inner);
        Set<String> refs = JarTreeShaker.extractReferencesFromBytecode(classWithLdc(outer));

        assertThat(refs).contains("com.example.Bootstrap", "com.example.Wrapper", "io.quarkus.core.Bootstraps",
                "com.example.Element");
    }

    @Test
    void plainStringConstantsAreNotTreatedAsDescriptors() {
        ConstantDynamic condy = new ConstantDynamic("name", "Ljava/lang/String;",
                new Handle(Opcodes.H_INVOKESTATIC, "com/example/Bootstrap", "name",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/String;",
                        false),
                "Literal;");
        Set<String> refs = JarTreeShaker.extractReferencesFromBytecode(classWithLdc(condy));

        assertThat(refs).contains("com.example.Bootstrap").doesNotContain("iteral", "Literal");
    }

    private static byte[] classWithLdc(Object constant) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "com/example/Generated", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "get",
                Type.getMethodDescriptor(Type.getType(Object.class)), null, null);
        method.visitCode();
        method.visitLdcInsn(constant);
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
}
