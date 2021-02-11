package io.quarkus.panache.common.deployment;

import org.objectweb.asm.AnnotationVisitor;

import io.quarkus.gizmo.Gizmo;
import io.quarkus.panache.common.deployment.EntityField.EntityFieldAnnotation;

/**
 * An AnnotationVisitor that intercepts and records annotations so that they can
 * be applied to a different element later
 */
public class PanacheMovingAnnotationVisitor extends AnnotationVisitor {

    private final EntityFieldAnnotation fieldAnno;

    public PanacheMovingAnnotationVisitor(EntityFieldAnnotation fieldAnno) {
        super(Gizmo.ASM_API_VERSION);
        this.fieldAnno = fieldAnno;
    }

    @Override
    public void visit(String name, Object value) {
        fieldAnno.attributes.put(name, value);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        EntityFieldAnnotation nestedAnno = new EntityFieldAnnotation(descriptor);
        fieldAnno.nestedAnnotations.add(nestedAnno);
        return new PanacheMovingAnnotationVisitor(nestedAnno);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        return this;
    }
}
