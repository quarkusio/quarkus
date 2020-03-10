package io.quarkus.panache.common.deployment;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.gizmo.Gizmo;

public class PanacheFieldAccessMethodVisitor extends MethodVisitor {

    private final String methodName;
    private String owner;
    private String methodDescriptor;
    private MetamodelInfo<?> modelInfo;

    public PanacheFieldAccessMethodVisitor(MethodVisitor methodVisitor, String owner,
            String methodName, String methodDescriptor,
            MetamodelInfo<?> modelInfo) {
        super(Gizmo.ASM_API_VERSION, methodVisitor);
        this.owner = owner;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
        this.modelInfo = modelInfo;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String fieldName, String descriptor) {
        String ownerName = owner.replace('/', '.');
        if ((opcode == Opcodes.GETFIELD
                || opcode == Opcodes.PUTFIELD)
                // if we're in the constructor, do not replace field accesses to this type and its supertypes
                // otherwise we risk running setters that depend on initialisation
                && (!this.methodName.equals("<init>")
                        || !targetIsInHierarchy(this.owner.replace('/', '.'), ownerName))
                && isEntityField(ownerName, fieldName)) {
            String methodName;
            String methodDescriptor;
            if (opcode == Opcodes.GETFIELD) {
                methodName = JavaBeanUtil.getGetterName(fieldName, descriptor);
                methodDescriptor = "()" + descriptor;
            } else {
                methodName = JavaBeanUtil.getSetterName(fieldName);
                methodDescriptor = "(" + descriptor + ")V";
            }
            if (!owner.equals(this.owner)
                    || !methodName.equals(this.methodName)
                    || !methodDescriptor.equals(this.methodDescriptor)) {
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, methodName, methodDescriptor, false);
            } else {
                // do not substitute to accessors inside its own accessor
                super.visitFieldInsn(opcode, owner, fieldName, descriptor);
            }
        } else {
            super.visitFieldInsn(opcode, owner, fieldName, descriptor);
        }
    }

    /**
     * Make sure that the target class is in the (superclass, since interfaces have no fields)
     * hierarchy of the current class
     */
    private boolean targetIsInHierarchy(String currentClass, String targetClass) {
        if (currentClass.equals(targetClass))
            return true;
        EntityModel<?> entityModel = modelInfo.getEntityModel(currentClass);
        if (entityModel == null)
            return false;
        if (entityModel.superClassName != null)
            return targetIsInHierarchy(entityModel.superClassName, targetClass);
        return false;
    }

    /**
     * Checks that the given field belongs to an entity (any entity)
     */
    boolean isEntityField(String className, String fieldName) {
        EntityModel<?> entityModel = modelInfo.getEntityModel(className);
        if (entityModel == null)
            return false;
        EntityField field = entityModel.fields.get(fieldName);
        if (field != null)
            return true;
        if (entityModel.superClassName != null)
            return isEntityField(entityModel.superClassName, fieldName);
        return false;
    }
}
