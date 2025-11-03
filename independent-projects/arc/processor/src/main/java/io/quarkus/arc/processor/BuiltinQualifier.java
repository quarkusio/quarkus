package io.quarkus.arc.processor;

import java.lang.constant.ClassDesc;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;

import org.jboss.jandex.AnnotationInstance;

import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.StaticFieldVar;
import io.quarkus.gizmo2.desc.FieldDesc;

enum BuiltinQualifier {

    DEFAULT(AnnotationInstance.create(DotNames.DEFAULT, null, Collections.emptyList()),
            Default.Literal.class.getName()),
    ANY(AnnotationInstance.create(DotNames.ANY, null, Collections.emptyList()),
            Any.Literal.class.getName()),;

    static final Set<AnnotationInstance> DEFAULT_QUALIFIERS = Set.of(DEFAULT.getInstance(), ANY.getInstance());

    private final AnnotationInstance instance;

    private final String literalType;

    private BuiltinQualifier(AnnotationInstance instance, String literalType) {
        this.instance = instance;
        this.literalType = literalType;
    }

    AnnotationInstance getInstance() {
        return instance;
    }

    StaticFieldVar getLiteralInstance() {
        ClassDesc literalClass = ClassDesc.of(literalType);
        return Expr.staticField(FieldDesc.of(literalClass, "INSTANCE", literalClass));
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
