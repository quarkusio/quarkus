package io.quarkus.arc.processor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.Type;

import io.quarkus.arc.processor.BuildExtension.BuildContext;

/**
 * Holds all {@link InjectionPointsTransformer} instances and provides helper method to apply them on a given
 * type. This is used in {@link InjectionPointInfo}, prior to creating actual injection points.
 *
 * In order to operate on up-to-date annotations, this class also leverages {@link AnnotationStore}.
 *
 * @author Matej Novotny
 */
public class InjectionPointModifier {

    private List<InjectionPointsTransformer> transformers;
    private BuildExtension.BuildContext buildContext;

    InjectionPointModifier(List<InjectionPointsTransformer> transformers, BuildExtension.BuildContext buildContext) {
        this.buildContext = buildContext;
        this.transformers = transformers;
    }

    public Set<AnnotationInstance> applyTransformers(Type type, AnnotationTarget target, Integer paramPosition,
            Set<AnnotationInstance> qualifiers) {
        // with no transformers, we just immediately return original set of qualifiers
        if (transformers.isEmpty()) {
            return qualifiers;
        }
        TransformationContextImpl transformationContext = new TransformationContextImpl(buildContext, target, qualifiers);
        for (InjectionPointsTransformer transformer : transformers) {
            if (transformer.appliesTo(type)) {
                transformer.transform(transformationContext);
            }
        }
        if (paramPosition != null && AnnotationTarget.Kind.METHOD.equals(target.kind())) {
            // only return set of qualifiers related to the given method parameter
            return transformationContext.getQualifiers().stream().filter(
                    annotationInstance -> target.asMethod().parameters().get(paramPosition).equals(annotationInstance.target()))
                    .collect(Collectors.toSet());
        } else {
            return transformationContext.getQualifiers();
        }
    }

    public Set<AnnotationInstance> applyTransformers(Type type, AnnotationTarget target, Set<AnnotationInstance> qualifiers) {
        return applyTransformers(type, target, null, qualifiers);
    }

    static class TransformationContextImpl extends AnnotationsTransformationContext<Set<AnnotationInstance>>
            implements InjectionPointsTransformer.TransformationContext {

        public TransformationContextImpl(BuildContext buildContext, AnnotationTarget target,
                Set<AnnotationInstance> annotations) {
            super(buildContext, target, annotations);
        }

        @Override
        public InjectionPointsTransformer.Transformation transform() {
            return new InjectionPointsTransformer.Transformation(new HashSet<>(getAnnotations()), getTarget(),
                    this::setAnnotations);
        }

        @Override
        public Set<AnnotationInstance> getQualifiers() {
            return getAnnotations();
        }

    }
}
