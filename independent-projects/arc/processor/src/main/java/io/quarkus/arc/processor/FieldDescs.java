package io.quarkus.arc.processor;

import io.quarkus.arc.impl.AnnotationLiterals;
import io.quarkus.arc.impl.Qualifiers;
import io.quarkus.gizmo2.desc.FieldDesc;

final class FieldDescs {
    private FieldDescs() {
    }

    static final FieldDesc QUALIFIERS_IP_QUALIFIERS = FieldDesc.of(Qualifiers.class, "IP_DEFAULT_QUALIFIERS");

    static final FieldDesc ANNOTATION_LITERALS_EMPTY_BOOLEAN_ARRAY = FieldDesc.of(AnnotationLiterals.class,
            "EMPTY_BOOLEAN_ARRAY");
    static final FieldDesc ANNOTATION_LITERALS_EMPTY_BYTE_ARRAY = FieldDesc.of(AnnotationLiterals.class,
            "EMPTY_BYTE_ARRAY");
    static final FieldDesc ANNOTATION_LITERALS_EMPTY_SHORT_ARRAY = FieldDesc.of(AnnotationLiterals.class,
            "EMPTY_SHORT_ARRAY");
    static final FieldDesc ANNOTATION_LITERALS_EMPTY_INT_ARRAY = FieldDesc.of(AnnotationLiterals.class,
            "EMPTY_INT_ARRAY");
    static final FieldDesc ANNOTATION_LITERALS_EMPTY_LONG_ARRAY = FieldDesc.of(AnnotationLiterals.class,
            "EMPTY_LONG_ARRAY");
    static final FieldDesc ANNOTATION_LITERALS_EMPTY_FLOAT_ARRAY = FieldDesc.of(AnnotationLiterals.class,
            "EMPTY_FLOAT_ARRAY");
    static final FieldDesc ANNOTATION_LITERALS_EMPTY_DOUBLE_ARRAY = FieldDesc.of(AnnotationLiterals.class,
            "EMPTY_DOUBLE_ARRAY");
    static final FieldDesc ANNOTATION_LITERALS_EMPTY_CHAR_ARRAY = FieldDesc.of(AnnotationLiterals.class,
            "EMPTY_CHAR_ARRAY");

    static final FieldDesc ANNOTATION_LITERALS_EMPTY_STRING_ARRAY = FieldDesc.of(AnnotationLiterals.class,
            "EMPTY_STRING_ARRAY");
    static final FieldDesc ANNOTATION_LITERALS_EMPTY_CLASS_ARRAY = FieldDesc.of(AnnotationLiterals.class,
            "EMPTY_CLASS_ARRAY");
}
