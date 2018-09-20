package org.jboss.protean.gizmo;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

public interface AnnotatedElement {

    AnnotationCreator addAnnotation(String annotationType);

    default AnnotationCreator addAnnotation(Class<?> annotationType) {
        return addAnnotation(annotationType.getName());
    }

    default void addAnnotation(AnnotationInstance annotation) {
        AnnotationCreator ac = addAnnotation(annotation.name().toString());
        for (AnnotationValue member : annotation.values()) {
            if (member.kind() == AnnotationValue.Kind.NESTED || member.kind() == AnnotationValue.Kind.ARRAY) {
                throw new RuntimeException("Not Yet Implemented: Cannot generate annotation " + annotation);
            } else if (member.kind() == AnnotationValue.Kind.BOOLEAN) {
                ac.addValue(member.name(), member.asBoolean());
            } else if (member.kind() == AnnotationValue.Kind.BYTE) {
                ac.addValue(member.name(), member.asByte());
            } else if (member.kind() == AnnotationValue.Kind.SHORT) {
                ac.addValue(member.name(), member.asShort());
            } else if (member.kind() == AnnotationValue.Kind.INTEGER) {
                ac.addValue(member.name(), member.asInt());
            } else if (member.kind() == AnnotationValue.Kind.LONG) {
                ac.addValue(member.name(), member.asLong());
            } else if (member.kind() == AnnotationValue.Kind.FLOAT) {
                ac.addValue(member.name(), member.asFloat());
            } else if (member.kind() == AnnotationValue.Kind.DOUBLE) {
                ac.addValue(member.name(), member.asDouble());
            } else if (member.kind() == AnnotationValue.Kind.STRING) {
                ac.addValue(member.name(), member.asString());
            }
        }
    }

}
