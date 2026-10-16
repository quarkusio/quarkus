package io.quarkus.kafka.client.deployment;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.gizmo.Gizmo;

/**
 * ASM {@link ClassVisitor} that rewrites {@code registerAppInfo} and {@code unregisterAppInfo}
 * in Kafka\'s {@code AppInfoParser} to skip JMX MBean registration/unregistration
 * while preserving the calls to {@code registerMetrics} and {@code unregisterMetrics}.
 * <p>
 * The original methods register/unregister JMX MBeans which causes "Error registering AppInfo mbean"
 * warnings in dev/test mode. This transformation replaces the method bodies to only delegate to
 * the internal metrics methods, removing the JMX side-effects.
 * <p>
 * The visitor detects the Kafka version by inspecting the {@code registerMetrics} method signature:
 * <ul>
 * <li>Kafka &ge; 4.2: {@code registerMetrics(Metrics, AppInfo, String)}</li>
 * <li>Kafka &lt; 4.2: {@code registerMetrics(Metrics, AppInfo)}</li>
 * </ul>
 */
class AppInfoClassVisitor extends ClassVisitor {

    private String appInfoParser;
    private String appInfoInner;
    private static final String METRICS = "org/apache/kafka/common/metrics/Metrics";

    private boolean hasClientIdParam;
    private final List<DeferredMethod> deferredMethods = new ArrayList<>();

    AppInfoClassVisitor(ClassVisitor classVisitor) {
        super(Gizmo.ASM_API_VERSION, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.appInfoParser = name;
        this.appInfoInner = name + "$AppInfo";
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
            String signature, String[] exceptions) {
        // Detect registerMetrics signature to determine Kafka version
        if ("registerMetrics".equals(name)) {
            Type[] argTypes = Type.getArgumentTypes(descriptor);
            // Kafka >= 4.2: registerMetrics(Metrics, AppInfo, String)
            // Kafka < 4.2:  registerMetrics(Metrics, AppInfo)
            hasClientIdParam = argTypes.length >= 3;
        }

        if ("registerAppInfo".equals(name) || "unregisterAppInfo".equals(name)) {
            // Defer these methods until visitEnd(), when we know the registerMetrics signature
            deferredMethods.add(new DeferredMethod(access, name, descriptor, signature, exceptions));
            // Return null to discard the original method body
            return null;
        }

        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        for (DeferredMethod m : deferredMethods) {
            MethodVisitor mv = super.visitMethod(m.access, m.name, m.descriptor, m.signature, m.exceptions);
            if (mv != null) {
                mv.visitCode();
                if ("registerAppInfo".equals(m.name)) {
                    generateRegisterAppInfo(mv);
                } else {
                    generateUnregisterAppInfo(mv);
                }
                mv.visitEnd();
            }
        }
        super.visitEnd();
    }

    /**
     * Generates the body for registerAppInfo(String prefix, String id, Metrics metrics, long nowMs):
     *
     * <pre>
     * // Kafka >= 4.2:
     * registerMetrics(metrics, new AppInfoParser.AppInfo(nowMs), id);
     *
     * // Kafka < 4.2:
     * registerMetrics(metrics, new AppInfoParser.AppInfo(nowMs));
     * </pre>
     */
    private void generateRegisterAppInfo(MethodVisitor mv) {
        // Local variables for registerAppInfo(String prefix, String id, Metrics metrics, long nowMs):
        // 0: prefix (String)
        // 1: id (String)
        // 2: metrics (Metrics)
        // 3-4: nowMs (long, takes 2 slots)

        // Push metrics (arg 2) onto stack
        mv.visitVarInsn(Opcodes.ALOAD, 2);

        // Create new AppInfoParser.AppInfo(nowMs)
        mv.visitTypeInsn(Opcodes.NEW, appInfoInner);
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.LLOAD, 3); // nowMs
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, appInfoInner, "<init>", "(J)V", false);

        if (hasClientIdParam) {
            // Kafka >= 4.2: registerMetrics(metrics, appInfo, id)
            mv.visitVarInsn(Opcodes.ALOAD, 1); // id
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, appInfoParser, "registerMetrics",
                    "(L" + METRICS + ";L" + appInfoInner + ";Ljava/lang/String;)V", false);
        } else {
            // Kafka < 4.2: registerMetrics(metrics, appInfo)
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, appInfoParser, "registerMetrics",
                    "(L" + METRICS + ";L" + appInfoInner + ";)V", false);
        }

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(hasClientIdParam ? 5 : 4, 5);
    }

    /**
     * Generates the body for unregisterAppInfo(String prefix, String id, Metrics metrics):
     *
     * <pre>
     * // Kafka >= 4.2:
     * unregisterMetrics(metrics, id);
     *
     * // Kafka < 4.2:
     * unregisterMetrics(metrics);
     * </pre>
     */
    private void generateUnregisterAppInfo(MethodVisitor mv) {
        // Local variables for unregisterAppInfo(String prefix, String id, Metrics metrics):
        // 0: prefix (String)
        // 1: id (String)
        // 2: metrics (Metrics)

        // Push metrics (arg 2)
        mv.visitVarInsn(Opcodes.ALOAD, 2);

        if (hasClientIdParam) {
            // Kafka >= 4.2: unregisterMetrics(metrics, id)
            mv.visitVarInsn(Opcodes.ALOAD, 1); // id
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, appInfoParser, "unregisterMetrics",
                    "(L" + METRICS + ";Ljava/lang/String;)V", false);
        } else {
            // Kafka < 4.2: unregisterMetrics(metrics)
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, appInfoParser, "unregisterMetrics",
                    "(L" + METRICS + ";)V", false);
        }

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(hasClientIdParam ? 2 : 1, 3);
    }

    private record DeferredMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    }
}
