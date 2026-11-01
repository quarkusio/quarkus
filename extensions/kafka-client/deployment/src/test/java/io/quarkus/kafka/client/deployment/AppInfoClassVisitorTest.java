package io.quarkus.kafka.client.deployment;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.assertj.core.api.Assertions.assertThat;

class AppInfoClassVisitorTest {

    private static final String LEGACY = "org/apache/kafka/common/utils/AppInfoParser";
    private static final String RELOCATED = "org/apache/kafka/common/utils/internals/AppInfoParser";
    private static final String METRICS = "org/apache/kafka/common/metrics/Metrics";

    @Test
    void rewritesKafkaPre42WithoutClientId() {
        assertRewrittenCalls(LEGACY, false);
    }

    @Test
    void rewritesKafka42WithClientId() {
        assertRewrittenCalls(LEGACY, true);
    }

    @Test
    void rewritesRelocatedKafka44WithClientId() {
        assertRewrittenCalls(RELOCATED, true);
    }

    private static void assertRewrittenCalls(String owner, boolean hasClientId) {
        String appInfo = owner + "$AppInfo";
        String registerDescriptor = "(L" + METRICS + ";L" + appInfo + ";"
                + (hasClientId ? "Ljava/lang/String;" : "") + ")V";
        String unregisterDescriptor = "(L" + METRICS + ";"
                + (hasClientId ? "Ljava/lang/String;" : "") + ")V";

        ClassWriter original = new ClassWriter(0);
        original.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        addEmptyMethod(original, "registerMetrics", registerDescriptor, hasClientId ? 3 : 2);
        addEmptyMethod(original, "unregisterMetrics", unregisterDescriptor, hasClientId ? 2 : 1);
        addEmptyMethod(original, "registerAppInfo",
                "(Ljava/lang/String;Ljava/lang/String;L" + METRICS + ";J)V", 5);
        addEmptyMethod(original, "unregisterAppInfo",
                "(Ljava/lang/String;Ljava/lang/String;L" + METRICS + ";)V", 3);
        original.visitEnd();

        ClassWriter transformed = new ClassWriter(0);
        new ClassReader(original.toByteArray()).accept(new AppInfoClassVisitor(transformed), 0);

        List<String> registrationCalls = new ArrayList<>();
        List<String> instantiatedTypes = new ArrayList<>();
        List<String> unregistrationCalls = new ArrayList<>();
        new ClassReader(transformed.toByteArray()).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                if (!name.equals("registerAppInfo") && !name.equals("unregisterAppInfo")) {
                    return null;
                }
                List<String> target = name.equals("registerAppInfo") ? registrationCalls : unregistrationCalls;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        if (opcode == Opcodes.NEW) {
                            instantiatedTypes.add(type);
                        }
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String invokedOwner, String invokedName,
                            String invokedDescriptor, boolean isInterface) {
                        target.add(invokedOwner + "." + invokedName + invokedDescriptor);
                    }
                };
            }
        }, 0);

        assertThat(instantiatedTypes).containsExactly(appInfo);
        assertThat(registrationCalls).containsExactly(
                appInfo + ".<init>(J)V",
                owner + ".registerMetrics" + registerDescriptor);
        assertThat(unregistrationCalls).containsExactly(owner + ".unregisterMetrics" + unregisterDescriptor);
    }

    private static void addEmptyMethod(ClassWriter writer, String name, String descriptor, int locals) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                name, descriptor, null, null);
        method.visitCode();
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, locals);
        method.visitEnd();
    }
}
