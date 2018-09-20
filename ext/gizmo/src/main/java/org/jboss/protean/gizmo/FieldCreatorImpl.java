package org.jboss.protean.gizmo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

class FieldCreatorImpl implements FieldCreator {

    private final FieldDescriptor fieldDescriptor;
    private final List<AnnotationCreatorImpl> annotations = new ArrayList<>();

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
        FieldVisitor fieldVisitor = file.visitField(modifiers, fieldDescriptor.getName(), fieldDescriptor.getType(), null, null);
        for(AnnotationCreatorImpl annotation : annotations) {
            AnnotationVisitor av = fieldVisitor.visitAnnotation(DescriptorUtils.extToInt(annotation.getAnnotationType()), true);
            for(Map.Entry<String, Object> e : annotation.getValues().entrySet()) {
                av.visit(e.getKey(), e.getValue());
            }
            av.visitEnd();
        }
        fieldVisitor.visitEnd();
    }

    @Override
    public AnnotationCreator addAnnotation(String annotationType) {
        AnnotationCreatorImpl ac = new AnnotationCreatorImpl(annotationType);
        annotations.add(ac);
        return ac;
    }
}
