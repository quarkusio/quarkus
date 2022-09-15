package io.quarkus.arc.processor;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.ResultHandle;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import java.util.Collections;
import org.jboss.jandex.AnnotationInstance;

enum BuiltinQualifier {

    DEFAULT(AnnotationInstance.create(DotNames.DEFAULT, null, Collections.emptyList()),
            Default.Literal.class.getName()),
    ANY(AnnotationInstance.create(DotNames.ANY, null, Collections.emptyList()),
            Any.Literal.class.getName()),;

    private final AnnotationInstance instance;

    private final String literalType;

    private BuiltinQualifier(AnnotationInstance instance, String literalType) {
        this.instance = instance;
        this.literalType = literalType;
    }

    AnnotationInstance getInstance() {
        return instance;
    }

    ResultHandle getLiteralInstance(BytecodeCreator creator) {
        return creator.readStaticField(FieldDescriptor.of(literalType, "INSTANCE", literalType));
    }

    static BuiltinQualifier of(AnnotationInstance instance) {
        for (BuiltinQualifier qualifier : values()) {
            if (qualifier.getInstance().name().equals(instance.name())) {
                return qualifier;
            }
        }
        return null;
    }

}
