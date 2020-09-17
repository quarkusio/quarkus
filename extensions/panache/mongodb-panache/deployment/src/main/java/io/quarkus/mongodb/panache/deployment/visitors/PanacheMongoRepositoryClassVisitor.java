package io.quarkus.mongodb.panache.deployment.visitors;

import static io.quarkus.deployment.util.AsmUtil.getDescriptor;
import static io.quarkus.mongodb.panache.deployment.BasePanacheMongoResourceProcessor.OBJECT_SIGNATURE;
import static java.util.Arrays.asList;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.mongodb.panache.deployment.ByteCodeType;
import io.quarkus.mongodb.panache.deployment.TypeBundle;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.visitors.PanacheRepositoryClassVisitor;

public class PanacheMongoRepositoryClassVisitor extends PanacheRepositoryClassVisitor {
    private static final ByteCodeType CLASS = new ByteCodeType(Class.class);

    private final TypeBundle typeBundle;

    public PanacheMongoRepositoryClassVisitor(String className, ClassVisitor outputClassVisitor,
            IndexView indexView, TypeBundle typeBundle) {
        super(className, outputClassVisitor, indexView);
        this.typeBundle = typeBundle;
    }

    @Override
    protected void generateModelBridge(MethodInfo method, AnnotationValue targetReturnTypeErased) {
        String descriptor = AsmUtil.getDescriptor(method, name -> typeArguments.get(name));
        String signature = AsmUtil.getSignature(method, name -> typeArguments.get(name));
        List<Type> parameters = method.parameters();

        // Note: we can't use SYNTHETIC here because otherwise Mockito will never mock these methods
        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC,
                method.name(),
                descriptor,
                signature,
                null);
        AsmUtil.copyParameterNames(mv, method);
        mv.visitFieldInsn(Opcodes.GETSTATIC, daoBinaryName, "operations",
                typeBundle.operations().descriptor());
        mv.visitCode();
        injectModel(mv);
        for (int i = 0; i < parameters.size(); i++) {
            mv.visitIntInsn(Opcodes.ALOAD, i + 1);
        }
        invokeOperation(mv, method);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void invokeOperation(MethodVisitor mv, MethodInfo method) {
        String operationDescriptor;
        Function<String, String> argMapper = type -> null;

        AnnotationInstance bridge = method.annotation(PanacheEntityEnhancer.DOTNAME_GENERATE_BRIDGE);
        AnnotationValue targetReturnTypeErased = bridge.value("targetReturnTypeErased");
        boolean erased = targetReturnTypeErased != null && targetReturnTypeErased.asBoolean();

        StringJoiner joiner = new StringJoiner("", "(", ")");
        joiner.add(CLASS.descriptor());
        for (Type parameter : method.parameters()) {
            joiner.add(getDescriptor(parameter, argMapper));
        }

        List<String> names = asList(typeBundle.queryType().dotName().toString(),
                typeBundle.updateType().dotName().toString());
        operationDescriptor = joiner +
                (erased || names.contains(method.returnType().name().toString())
                        ? OBJECT_SIGNATURE
                        : getDescriptor(method.returnType(), argMapper));

        mv.visitMethodInsn(INVOKEVIRTUAL, typeBundle.operations().internalName(), method.name(),
                operationDescriptor, false);
        if (method.returnType().kind() != Type.Kind.PRIMITIVE) {
            Type type = method.returnType();
            String cast;
            if (erased) {
                cast = entityBinaryType;
            } else {
                cast = type.name().toString().replace('.', '/');
            }
            mv.visitTypeInsn(CHECKCAST, cast);
        }
        mv.visitInsn(AsmUtil.getReturnInstruction(method.returnType()));
    }

    @Override
    protected DotName getPanacheRepositoryDotName() {
        return typeBundle.repository().dotName();
    }

    @Override
    protected DotName getPanacheRepositoryBaseDotName() {
        return typeBundle.repositoryBase().dotName();
    }

    @Override
    protected String getPanacheOperationsInternalName() {
        return typeBundle.operations().internalName();
    }
}
