package io.quarkus.resteasy.reactive.links.deployment;

import static io.quarkus.gizmo.DescriptorUtils.objectToDescriptor;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.IRETURN;

import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import io.quarkus.gizmo.Gizmo;

class GetterImplementor extends ClassVisitor {

    private final GetterMetadata getterMetadata;

    static BiFunction<String, ClassVisitor, ClassVisitor> getVisitorFunction(GetterMetadata getterMetadata) {
        return (className, classVisitor) -> new GetterImplementor(classVisitor, getterMetadata);
    }

    GetterImplementor(ClassVisitor outputClassVisitor, GetterMetadata getterMetadata) {
        super(Gizmo.ASM_API_VERSION, outputClassVisitor);
        this.getterMetadata = getterMetadata;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        generateGetter();
        super.visitEnd();
    }

    private void generateGetter() {
        String owner = Type.getType(objectToDescriptor(getterMetadata.getEntityType())).getInternalName();
        String fieldDescriptor = objectToDescriptor(getterMetadata.getFieldType());
        String getterDescriptor = "()" + fieldDescriptor;

        MethodVisitor mv = super.visitMethod(ACC_PUBLIC, getterMetadata.getGetterName(), getterDescriptor, null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, owner, getterMetadata.getFieldName(), fieldDescriptor);
        mv.visitInsn(Type.getType(fieldDescriptor).getOpcode(IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
