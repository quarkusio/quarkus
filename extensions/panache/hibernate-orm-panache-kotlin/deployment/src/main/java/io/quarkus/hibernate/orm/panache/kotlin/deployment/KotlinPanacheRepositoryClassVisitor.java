package io.quarkus.hibernate.orm.panache.kotlin.deployment;

import static io.quarkus.deployment.util.AsmUtil.autobox;
import static io.quarkus.gizmo.Gizmo.ASM_API_VERSION;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.JPA_OPERATIONS;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.PANACHE_REPOSITORY_BASE;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.sanitize;
import static io.quarkus.panache.common.deployment.visitors.PanacheRepositoryClassVisitor.findEntityTypeArgumentsForPanacheRepository;
import static org.jboss.jandex.DotName.createSimple;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Type.getArgumentTypes;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getReturnType;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;

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
    protected Map<String, String> typeArguments = new HashMap<>();
    private String idBinaryType;
    private String idSignature;

    public KotlinPanacheRepositoryClassVisitor(String className, ClassVisitor outputClassVisitor, IndexView indexView) {
        super(Gizmo.ASM_API_VERSION, outputClassVisitor);
        this.indexView = indexView;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);

        final String repositoryClassName = name.replace('/', '.');

        String[] foundTypeArguments = findEntityTypeArgumentsForPanacheRepository(indexView, repositoryClassName,
                PANACHE_REPOSITORY_BASE);

        String entityBinaryType = foundTypeArguments[0];
        entitySignature = "L" + entityBinaryType + ";";
        entityType = Type.getType(entitySignature);
        idBinaryType = foundTypeArguments[1];
        idSignature = "L" + idBinaryType + ";";

        typeArguments.put("Entity", entitySignature);
        typeArguments.put("Id", idSignature);
        indexView.getClassByName(createSimple(repositoryClassName))
                .methods()
                .forEach(method -> {
                    if (method.hasAnnotation(PanacheEntityEnhancer.DOTNAME_GENERATE_BRIDGE)) {
                        bridgeMethods.put(method.name() + AsmUtil.getDescriptor(method, m -> typeArguments.get(m)), method);
                    }
                });
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (bridgeMethods.get(name + descriptor) != null) {
            final Type[] argumentTypes = getArgumentTypes(descriptor);
            int primitive = -1;

            for (int i = 0; i < argumentTypes.length; i++) {
                if (argumentTypes[i].getInternalName().length() == 1) {
                    primitive = i;
                }
            }

            int finalPrimitive = primitive;
            mv = new MethodVisitor(ASM_API_VERSION, mv) {
                /**
                 * This method tracks the location of any primitive value (ID types) passed in. The javac-generated
                 * bytecode is expecting an Object but with non-nullable "primitive" types in kotlin, those are
                 * expressed as primitives and not the wrapper types (e.g., Long). This method will catch the cases
                 * where the ID values passed in are loaded on to the stack prior to invoking the call to JpaOperations.
                 * It will then inject an autoboxing-like call to ensure that any long values because Long values.
                 */
                @Override
                public void visitVarInsn(int opcode, int var) {
                    if (opcode == ALOAD && var == 0) {
                        visitLdcInsn(entityType);
                    } else {
                        super.visitVarInsn(opcode, var);
                        if (opcode == LLOAD && var == (finalPrimitive + 1)) {
                            Type wrapper = autobox(argumentTypes[finalPrimitive]);
                            super.visitMethodInsn(INVOKESTATIC, wrapper.getInternalName(), "valueOf",
                                    getMethodDescriptor(wrapper, argumentTypes[finalPrimitive]), false);

                        }
                    }
                }

                /**
                 * This method redirects the kotlinc generated call to $DefaultImpls to JpaOperations. In case of IDs,
                 * e.g., these parameters are represented using primitives by kotlinc since they can never be null.
                 * JpaOperation expects an object reference so we need to update the descriptor for the method
                 * invocation to reflect that rather than the primitive type.
                 */
                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                        boolean isInterface) {
                    if (opcode == INVOKESTATIC && DEFAULT_IMPLS.matcher(owner).matches()) {

                        owner = JPA_OPERATIONS;
                        Type[] arguments = getArgumentTypes(descriptor);
                        sanitize(arguments);
                        descriptor = getMethodDescriptor(getReturnType(descriptor), arguments);
                    }

                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
            };

        }
        return mv;
    }

}
