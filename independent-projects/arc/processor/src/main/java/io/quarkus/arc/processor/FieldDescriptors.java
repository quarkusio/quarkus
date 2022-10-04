package io.quarkus.arc.processor;

import java.util.Set;

import io.quarkus.arc.impl.AnnotationLiterals;
import io.quarkus.arc.impl.Qualifiers;
import io.quarkus.gizmo.FieldDescriptor;

/**
 *
 */
final class FieldDescriptors {

    static final FieldDescriptor QUALIFIERS_IP_QUALIFIERS = FieldDescriptor.of(Qualifiers.class, "IP_DEFAULT_QUALIFIERS",
            Set.class);

    static final FieldDescriptor ANNOTATION_LITERALS_EMPTY_BOOLEAN_ARRAY = FieldDescriptor.of(AnnotationLiterals.class,
            "EMPTY_BOOLEAN_ARRAY", boolean[].class);
    static final FieldDescriptor ANNOTATION_LITERALS_EMPTY_BYTE_ARRAY = FieldDescriptor.of(AnnotationLiterals.class,
            "EMPTY_BYTE_ARRAY", byte[].class);
    static final FieldDescriptor ANNOTATION_LITERALS_EMPTY_SHORT_ARRAY = FieldDescriptor.of(AnnotationLiterals.class,
            "EMPTY_SHORT_ARRAY", short[].class);
    static final FieldDescriptor ANNOTATION_LITERALS_EMPTY_INT_ARRAY = FieldDescriptor.of(AnnotationLiterals.class,
            "EMPTY_INT_ARRAY", int[].class);
    static final FieldDescriptor ANNOTATION_LITERALS_EMPTY_LONG_ARRAY = FieldDescriptor.of(AnnotationLiterals.class,
            "EMPTY_LONG_ARRAY", long[].class);
    static final FieldDescriptor ANNOTATION_LITERALS_EMPTY_FLOAT_ARRAY = FieldDescriptor.of(AnnotationLiterals.class,
            "EMPTY_FLOAT_ARRAY", float[].class);
    static final FieldDescriptor ANNOTATION_LITERALS_EMPTY_DOUBLE_ARRAY = FieldDescriptor.of(AnnotationLiterals.class,
            "EMPTY_DOUBLE_ARRAY", double[].class);
    static final FieldDescriptor ANNOTATION_LITERALS_EMPTY_CHAR_ARRAY = FieldDescriptor.of(AnnotationLiterals.class,
            "EMPTY_CHAR_ARRAY", char[].class);

    static final FieldDescriptor ANNOTATION_LITERALS_EMPTY_STRING_ARRAY = FieldDescriptor.of(AnnotationLiterals.class,
            "EMPTY_STRING_ARRAY", String[].class);
    static final FieldDescriptor ANNOTATION_LITERALS_EMPTY_CLASS_ARRAY = FieldDescriptor.of(AnnotationLiterals.class,
            "EMPTY_CLASS_ARRAY", Class[].class);

    private FieldDescriptors() {
    }

}
