package io.quarkus.arc.processor;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
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
         * Returns {@link AnnotationTarget} representing this injection point.
         *
         * @return the annotation target of this injection point
         */
        AnnotationTarget getTarget();

        /**
         * Returns current set of annotations instances - qualifiers.
         *
         * @return the annotation instances
         */
        Set<AnnotationInstance> getQualifiers();

        /**
         * Retrieves all annotations attached to the {@link AnnotationTarget} that this transformer operates on
         * even if they were altered by {@code AnnotationsTransformer}. This method is preferred to manual inspection
         * of {@link AnnotationTarget} which may, in some corner cases, hold outdated information.
         *
         * The resulting set of annotations contains all annotations, not just CDI qualifiers.
         * If the annotation target is a method, then this set contains annotations that belong to the method itself
         * as well as to its parameters.
         *
         * @return collection of all annotations related to given {@link AnnotationTarget}
         */
        Collection<AnnotationInstance> getAllAnnotations();

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
