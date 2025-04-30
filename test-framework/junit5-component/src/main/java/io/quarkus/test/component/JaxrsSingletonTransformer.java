package io.quarkus.test.component;

import java.util.List;

import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuiltinScope;

/**
 * Add {@link Singleton} to a JAX-RS component that has no scope annotation.
 */
public class JaxrsSingletonTransformer implements AnnotationsTransformer {

    private final List<DotName> ANNOTATIONS = List.of(DotName.createSimple("jakarta.ws.rs.Path"),
            DotName.createSimple("jakarta.ws.rs.ApplicationPath"), DotName.createSimple("jakarta.ws.rs.ext.Provider"));

    @Override
    public boolean appliesTo(Kind kind) {
        return Kind.CLASS == kind;
    }

    @Override
    public void transform(TransformationContext context) {
        // Note that custom scopes are not supported yet
        if (BuiltinScope.isIn(context.getAnnotations())) {
            return;
        }
        if (Annotations.containsAny(context.getAnnotations(), ANNOTATIONS)) {
            context.transform().add(Singleton.class).done();
        }
    }

}
