package io.quarkus.arc.processor;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;

import org.jboss.jandex.AnnotationInstance;

import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.StaticFieldVar;
import io.quarkus.gizmo2.desc.FieldDesc;

enum BuiltinQualifier {

    DEFAULT(AnnotationInstance.create(DotNames.DEFAULT, null, List.of()), Default.Literal.class),
    ANY(AnnotationInstance.create(DotNames.ANY, null, List.of()), Any.Literal.class),
    ;

    static final Set<AnnotationInstance> DEFAULT_QUALIFIERS = Set.of(DEFAULT.getInstance(), ANY.getInstance());

    private final AnnotationInstance instance;

    private final ClassDesc literalClass;

    BuiltinQualifier(AnnotationInstance instance, Class<? extends Annotation> literalClass) {
        this.instance = instance;
        this.literalClass = ClassDesc.of(literalClass.getName());
    }

    AnnotationInstance getInstance() {
        return instance;
    }

    StaticFieldVar getLiteralInstance() {
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
