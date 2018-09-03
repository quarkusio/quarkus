package org.jboss.protean.gizmo;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

class FieldCreatorImpl implements FieldCreator {

    private final FieldDescriptor fieldDescriptor;

    private int modifiers;

    public FieldCreatorImpl(FieldDescriptor fieldDescriptor) {
        this.fieldDescriptor = fieldDescriptor;
        this.modifiers = Opcodes.ACC_PRIVATE;
    }

    @Override
    public FieldDescriptor getFieldDescriptor() {
        return fieldDescriptor;
    }

    @Override
    public int getModifiers() {
        return modifiers;
    }

    @Override
    public FieldCreator setModifiers(int modifiers) {
        this.modifiers = modifiers;
        return this;
    }

    @Override
    public void write(ClassWriter file) {
        file.visitField(modifiers, fieldDescriptor.getName(), fieldDescriptor.getType(), null, null);
    }

}
