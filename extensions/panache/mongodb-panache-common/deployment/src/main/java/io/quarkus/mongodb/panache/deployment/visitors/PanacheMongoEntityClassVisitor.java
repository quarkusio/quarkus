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
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.mongodb.panache.deployment.ByteCodeType;
import io.quarkus.mongodb.panache.deployment.TypeBundle;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.visitors.PanacheEntityClassVisitor;

public class PanacheMongoEntityClassVisitor extends PanacheEntityClassVisitor<EntityField> {
    private static final ByteCodeType CLASS = new ByteCodeType(Class.class);

    private final TypeBundle typeBundle;

    public PanacheMongoEntityClassVisitor(String className,
            ClassVisitor outputClassVisitor,
            MetamodelInfo<EntityModel<EntityField>> modelInfo,
            ClassInfo panacheEntityBaseClassInfo,
            ClassInfo entityInfo,
            List<PanacheMethodCustomizer> methodCustomizers,
            TypeBundle typeBundle) {
        super(className, outputClassVisitor, modelInfo, panacheEntityBaseClassInfo, entityInfo, methodCustomizers);
        this.typeBundle = typeBundle;
    }

    @Override
    protected void generateMethod(MethodInfo method, AnnotationValue targetReturnTypeErased) {
        String descriptor = AsmUtil.getDescriptor(method, name -> null);
        String signature = AsmUtil.getSignature(method, name -> null);
        List<org.jboss.jandex.Type> parameters = method.parameters();

        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                method.name(),
                descriptor,
                signature,
                null);
        AsmUtil.copyParameterNames(mv, method);
        mv.visitCode();
        for (PanacheMethodCustomizer customizer : methodCustomizers) {
            customizer.customize(thisClass, method, mv);
        }
        mv.visitFieldInsn(Opcodes.GETSTATIC, thisClass.getInternalName(), "operations",
                typeBundle.operations().descriptor());
        injectModel(mv);
        for (int i = 0; i < parameters.size(); i++) {
            mv.visitIntInsn(Opcodes.ALOAD, i);
        }
        invokeOperation(mv, method, method.parameters());
        mv.visitMaxs(0, 0);
        mv.visitEnd();

    }

    private void invokeOperation(MethodVisitor mv, MethodInfo method, List<Type> parameters) {
        String operationDescriptor;
        Function<String, String> argMapper = type -> null;

        AnnotationInstance bridge = method.annotation(PanacheEntityEnhancer.DOTNAME_GENERATE_BRIDGE);
        AnnotationValue targetReturnTypeErased = bridge.value("targetReturnTypeErased");
        boolean erased = targetReturnTypeErased != null && targetReturnTypeErased.asBoolean();

        StringJoiner joiner = new StringJoiner("", "(", ")");
        joiner.add(CLASS.descriptor());
        for (Type parameter : parameters) {
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
                cast = thisClass.getInternalName();
            } else {
                cast = type.name().toString().replace('.', '/');
            }
            mv.visitTypeInsn(CHECKCAST, cast);
        }
        mv.visitInsn(AsmUtil.getReturnInstruction(method.returnType()));
    }

    @Override
    protected String getPanacheOperationsInternalName() {
        return typeBundle.operations().internalName();
    }

    @Override
    protected void generateAccessorSetField(MethodVisitor mv, EntityField field) {
        mv.visitFieldInsn(Opcodes.PUTFIELD, thisClass.getInternalName(), field.name, field.descriptor);
    }

    @Override
    protected void generateAccessorGetField(MethodVisitor mv, EntityField field) {
        mv.visitFieldInsn(Opcodes.GETFIELD, thisClass.getInternalName(), field.name, field.descriptor);
    }
}
