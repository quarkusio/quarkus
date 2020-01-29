package io.quarkus.arc.processor;

import io.quarkus.arc.impl.Qualifiers;
import io.quarkus.gizmo.FieldDescriptor;
import java.util.Set;

/**
 *
 */
final class FieldDescriptors {

    static final FieldDescriptor QUALIFIERS_IP_QUALIFIERS = FieldDescriptor.of(Qualifiers.class, "IP_DEFAULT_QUALIFIERS",
            Set.class);

    private FieldDescriptors() {
    }

}
