package io.quarkiverse.acme.deployment;

import org.acme.AnnotationAddedByExtension;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import io.quarkus.gizmo.Gizmo;

public class AnnotationAdjuster extends ClassVisitor {

    private static final String SIMPLE_ANNOTATION_TYPENAME = "L" + AnnotationAddedByExtension.class.getName().replace('.', '/')
            + ";";

    public AnnotationAdjuster(ClassVisitor visitor, String className) {
        super(Gizmo.ASM_API_VERSION, visitor);

    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        AnnotationVisitor av = visitAnnotation(SIMPLE_ANNOTATION_TYPENAME, true);
        Type value = Type.getType(AnnotationAddedByExtension.class);
        if (av != null) {
            av.visit("value", value);
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

}
