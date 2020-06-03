package io.quarkus.hibernate.orm.panache.kotlin.deployment;

import static io.quarkus.gizmo.Gizmo.ASM_API_VERSION;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.CLASS_SIGNATURE;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.JPA_OPERATIONS;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.OBJECT_SIGNATURE;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.PANACHE_COMPANION_SIGNATURE;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.PANACHE_ENTITY_BASE_SIGNATURE;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.PANACHE_ENTITY_SIGNATURE;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;

/**
 * kotlinc compiles default methods in to the implementing classes so we don't need to generate whole method bodies.
 * This visitor, then, inspects those methods and replaces the calls to the $DefaultImpls class (which holds those
 * default method implementations) with ones to JpaOperations thus giving the method actual functionality.
 */
class KotlinPanacheCompanionClassVisitor extends ClassVisitor {

    private static final String DEFAULT_IMPLS = "io/quarkus/hibernate/orm/panache/kotlin/PanacheCompanion$DefaultImpls";
    private final Map<String, MethodInfo> bridgeMethods = new TreeMap<>();
    private String entityBinaryType;
    private String entitySignature;
    private org.objectweb.asm.Type entityType;

    public KotlinPanacheCompanionClassVisitor(ClassVisitor outputClassVisitor, ClassInfo entityInfo) {
        super(ASM_API_VERSION, outputClassVisitor);

        entityInfo
                .methods()
                .forEach(method -> {
                    if (method.hasAnnotation(PanacheEntityEnhancer.DOTNAME_GENERATE_BRIDGE)) {
                        bridgeMethods.put(method.name() + AsmUtil.getDescriptor(method, m -> null), method);
                    }
                });
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);

        String className = name.replace('.', '/');

        entityBinaryType = className.replace("$Companion", "");
        entitySignature = "L" + entityBinaryType + ";";
        entityType = org.objectweb.asm.Type.getType(entitySignature);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (bridgeMethods.get(name + descriptor) != null) {
            mv = new MethodVisitor(ASM_API_VERSION, mv) {
                @Override
                public void visitVarInsn(int opcode, int var) {
                    if (opcode == ALOAD && var == 0) {
                        visitLdcInsn(entityType);
                    } else {
                        super.visitVarInsn(opcode, var);
                    }
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    if (opcode == INVOKESTATIC && owner.equals(DEFAULT_IMPLS)) {
                        String replace = descriptor
                                .replace(PANACHE_ENTITY_SIGNATURE, OBJECT_SIGNATURE)
                                .replace(PANACHE_ENTITY_BASE_SIGNATURE, OBJECT_SIGNATURE)
                                .replace(PANACHE_COMPANION_SIGNATURE, CLASS_SIGNATURE);
                        super.visitMethodInsn(opcode, JPA_OPERATIONS, name, replace, isInterface);
                    } else {
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }
                }
            };
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        if (cv != null) {
            cv.visitEnd();
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", KotlinPanacheCompanionClassVisitor.class.getSimpleName() + "[", "]")
                .add(entityBinaryType)
                .toString();
    }
}
