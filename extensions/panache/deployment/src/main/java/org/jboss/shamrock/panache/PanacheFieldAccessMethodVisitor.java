package org.jboss.shamrock.panache;

import java.util.Map;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class PanacheFieldAccessMethodVisitor extends MethodVisitor {

    private final String methodName;
    private Map<String, EntityModel> entities;
    private String owner;
    private String methodDescriptor;

    PanacheFieldAccessMethodVisitor(MethodVisitor methodVisitor, String owner, 
                                    String methodName, String methodDescriptor,
                                    Map<String, EntityModel> entities) {
        super(Opcodes.ASM6, methodVisitor);
        this.owner = owner;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
        this.entities = entities;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String fieldName, String descriptor) {
        // FIXME: do not substitute to accessors to this or super fields inside constructor?
        if((opcode == Opcodes.GETFIELD
                || opcode == Opcodes.PUTFIELD)
                && isEntityField(owner.replace('/', '.'), fieldName)) {
            System.err.println("Visiting field access to "+owner+"."+fieldName+" in method "+methodName);
            String methodName;
            String methodDescriptor;
            if(opcode == Opcodes.GETFIELD) {
                methodName = JavaBeanUtil.getGetterName(fieldName, descriptor);
                methodDescriptor = "()"+descriptor;
            } else {
                methodName = JavaBeanUtil.getSetterName(fieldName);
                methodDescriptor = "("+descriptor+")V";
            }
            if(!owner.equals(this.owner)
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
     * @param className a dot-separated class name
     */
    boolean isEntityField(String className, String fieldName) {
        EntityModel entityModel = entities.get(className);
        if(entityModel == null)
            return false;
        EntityField field = entityModel.fields.get(fieldName);
        if(field != null)
            return true;
        if(entityModel.superClassName != null)
            return isEntityField(entityModel.superClassName, fieldName);
        return false;
    }
}