package io.quarkus.arc.processor;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Type;

/**
 * Allows a build-time extension to alter qualifiers on an injection point.
 * <p>
 *
 * @author Matej Novotny
 */
public interface InjectionPointsTransformer extends BuildExtension {

    /**
     * Returns true if this transformer is meant to be applied to the supplied {@code requiredType}.
     *
     * @param requiredType the declared type of the injection point
     */
    boolean appliesTo(Type requiredType);

    /**
     * Method is invoked for each injection point that returns true from {@link #appliesTo(Type)}.
     * For further filtering (declaring class, qualifiers present and so on), user can use helper methods
     * present within {@link TransformationContext}.
     *
     * @param transformationContext
     */
    void transform(TransformationContext transformationContext);

    interface TransformationContext extends BuildExtension.BuildContext {

        /**
         * Returns the {@link AnnotationTarget} of this injection point. That is, a {@link FieldInfo}
         * for an injected field, or a {@link org.jboss.jandex.MethodParameterInfo} for an injected
         * method parameter. Returns {@code null} in case of a synthetic injection point.
         *
         * @return the annotation target or {@code null} in case of synthetic injection point
         */
        AnnotationTarget getAnnotationTarget();

        /**
         * Returns current set of annotations instances - qualifiers.
         *
         * @return the annotation instances
         */
        Set<AnnotationInstance> getQualifiers();

        /**
         * Retrieves all annotations attached to the {@link AnnotationTarget} that this transformer operates on.
         * This {@link AnnotationTarget} is equal to what the {@link #getAnnotationTarget()} method returns.
         * <p>
         * The result includes annotations that were altered by {@code AnnotationsTransformer}.
         * This method is preferred to manual inspection of {@link AnnotationTarget} which may, in some corner cases,
         * hold outdated information.
         * <p>
         * The resulting set of annotations contains all annotations, not just CDI qualifiers.
         *
         * @return collection of all annotations related to given {@link AnnotationTarget}
         */
        Collection<AnnotationInstance> getAllTargetAnnotations();

        /**
         * The transformation is not applied until the {@link Transformation#done()} method is invoked.
         *
         * @return a new transformation
         */
        Transformation transform();

    }

    final class Transformation extends AbstractAnnotationsTransformation<Transformation, Set<AnnotationInstance>> {

        Transformation(Set<AnnotationInstance> annotations, AnnotationTarget target,
                Consumer<Set<AnnotationInstance>> transformationConsumer) {
            super(annotations, target, transformationConsumer);
        }

        @Override
        protected Transformation self() {
            return this;
        }

    }

}
