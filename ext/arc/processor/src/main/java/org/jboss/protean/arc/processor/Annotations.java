package org.jboss.protean.arc.processor;

import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.MethodInfo;

public final class Annotations {

    private Annotations() {
    }

    static Object convertAnnotationValue(AnnotationValue value, MethodInfo method) {
        if (value.kind().equals(org.jboss.jandex.AnnotationValue.Kind.ARRAY)) {
            // Array members must be Nonbinding
            return "new " + method.returnType().asArrayType().component().name() + "[]{}";
        } else if (value.kind().equals(org.jboss.jandex.AnnotationValue.Kind.BOOLEAN)) {
            return value.asBoolean();
        } else if (value.kind().equals(org.jboss.jandex.AnnotationValue.Kind.STRING)) {
            return "\"" + value.asString() + "\"";
        } else if (value.kind().equals(org.jboss.jandex.AnnotationValue.Kind.ENUM)) {
            return Types.convertNested(method.returnType().name()) + "." + value.asEnum();
        } else {
            return value.toString();
        }
    }

}
