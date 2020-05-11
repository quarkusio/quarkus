package io.quarkus.hibernate.orm.panache.kotlin.deployment;

import static io.quarkus.gizmo.Gizmo.ASM_API_VERSION;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.CLASS_SIGNATURE;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.ID_TYPE_SIGNATURE;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.JPA_OPERATIONS;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.OBJECT_SIGNATURE;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.PANACHE_ENTITY_SIGNATURE;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.PANACHE_REPOSITORY_BASE_DOTNAME;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.PANACHE_REPOSITORY_BASE_SIGNATURE;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.PANACHE_REPOSITORY_SIGNATURE;
import static io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer.PanacheRepositoryClassVisitor.findEntityTypeArgumentsForPanacheRepository;
import static org.jboss.jandex.DotName.createSimple;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import io.quarkus.gizmo.Gizmo;
import io.quarkus.panache.common.deployment.JandexUtil;

/**
 * kotlinc compiles default methods in to the implementing classes so we don't need to generate whole method bodies.
 * This visitor, then, inspects those methods and replaces the calls to the $DefaultImpls class (which holds those
 * default method implementations) with ones to JpaOperations thus giving the method actual functionality.
 */
class KotlinPanacheRepositoryClassVisitor extends ClassVisitor {

    public static final Pattern DEFAULT_IMPLS = Pattern.compile(".*PanacheRepository.*\\$DefaultImpls");
    private final Map<String, MethodInfo> bridgeMethods = new TreeMap<>();
    private final IndexView indexView;
    private org.objectweb.asm.Type entityType;
    private String entitySignature;

    public KotlinPanacheRepositoryClassVisitor(String className, ClassVisitor outputClassVisitor, IndexView indexView) {
        super(Gizmo.ASM_API_VERSION, outputClassVisitor);
        this.indexView = indexView;
        indexView.getClassByName(createSimple(className))
                .methods()
                .forEach(method -> {
                    if (method.hasAnnotation(JandexUtil.DOTNAME_GENERATE_BRIDGE)) {
                        bridgeMethods.put(method.name() + JandexUtil.getDescriptor(method, m -> null), method);
                    }
                });
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);

        final String repositoryClassName = name.replace('/', '.');

        String[] foundTypeArguments = findEntityTypeArgumentsForPanacheRepository(indexView, repositoryClassName,
                PANACHE_REPOSITORY_BASE_DOTNAME);

        String entityBinaryType = foundTypeArguments[0];
        entitySignature = "L" + entityBinaryType + ";";
        entityType = Type.getType(entitySignature);
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
                    if (opcode == INVOKESTATIC && DEFAULT_IMPLS.matcher(owner).matches()) {
                        String replace = descriptor
                                .replace(PANACHE_REPOSITORY_BASE_SIGNATURE, CLASS_SIGNATURE)
                                .replace(PANACHE_REPOSITORY_SIGNATURE, CLASS_SIGNATURE)
                                .replace(PANACHE_ENTITY_SIGNATURE, OBJECT_SIGNATURE)
                                .replace(ID_TYPE_SIGNATURE, OBJECT_SIGNATURE);
                        super.visitMethodInsn(opcode, JPA_OPERATIONS, name, replace, isInterface);
                    } else {
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }
                }
            };

        }
        return mv;
    }
}
