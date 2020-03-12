package io.quarkus.arc.processor;

import io.quarkus.arc.impl.AnnotationLiterals;
import io.quarkus.arc.impl.Qualifiers;
import io.quarkus.gizmo.FieldDescriptor;
import java.util.Set;

/**
 *
 */
final class FieldDescriptors {

    static final FieldDescriptor QUALIFIERS_IP_QUALIFIERS = FieldDescriptor.of(Qualifiers.class, "IP_DEFAULT_QUALIFIERS",
            Set.class);

    static final FieldDescriptor ANNOTATION_LITERALS_EMPTY_CLASS_ARRAY = FieldDescriptor.of(AnnotationLiterals.class,
            "EMPTY_CLASS_ARRAY",
            Class[].class);

    static final FieldDescriptor ANNOTATION_LITERALS_EMPTY_STRING_ARRAY = FieldDescriptor.of(AnnotationLiterals.class,
            "EMPTY_STRING_ARRAY",
            String[].class);

    static final FieldDescriptor ANNOTATION_LITERALS_EMPTY_LONG_ARRAY = FieldDescriptor.of(AnnotationLiterals.class,
            "EMPTY_LONG_ARRAY",
            long[].class);

    static final FieldDescriptor ANNOTATION_LITERALS_EMPTY_INT_ARRAY = FieldDescriptor.of(AnnotationLiterals.class,
            "EMPTY_INT_ARRAY",
            int[].class);

    private FieldDescriptors() {
    }

}
