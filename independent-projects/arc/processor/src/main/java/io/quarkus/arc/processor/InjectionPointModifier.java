package io.quarkus.arc.processor;

import io.quarkus.arc.processor.BuildExtension.BuildContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.Type;

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

    public Set<AnnotationInstance> applyTransformers(Type type, AnnotationTarget target, Set<AnnotationInstance> qualifiers) {
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
        return transformationContext.getQualifiers();
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
