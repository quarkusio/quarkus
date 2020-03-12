package io.quarkus.arc.processor;

import java.util.Collection;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;

/**
 * Allows a build-time extension to override the annotations that exist on bean classes.
 * <p>
 * The container should use {@link AnnotationStore} to obtain annotations of any {@link org.jboss.jandex.ClassInfo},
 * {@link org.jboss.jandex.FieldInfo} and {@link org.jboss.jandex.MethodInfo}.
 *
 * @author Martin Kouba
 */
public interface AnnotationsTransformer extends BuildExtension {

    /**
     * By default, the transformation is applied to all kinds of targets.
     *
     * @param kind
     * @return {@code true} if the transformation applies to the specified kind, {@code false} otherwise
     */
    default boolean appliesTo(Kind kind) {
        return true;
    }

    /**
     *
     * @param transformationContext
     */
    void transform(TransformationContext transformationContext);

    interface TransformationContext extends BuildContext {

        AnnotationTarget getTarget();

        /**
         * The initial set of annotations instances corresponds to {@link org.jboss.jandex.ClassInfo#classAnnotations()},
         * {@link org.jboss.jandex.FieldInfo#annotations()} and {@link org.jboss.jandex.MethodInfo#annotations()} respectively.
         * 
         * @return the annotation instances
         */
        Collection<AnnotationInstance> getAnnotations();

        /**
         * The transformation is not applied until the {@link Transformation#done()} method is invoked.
         * 
         * @return a new transformation
         */
        Transformation transform();

        default boolean isClass() {
            return getTarget().kind() == Kind.CLASS;
        }

        default boolean isField() {
            return getTarget().kind() == Kind.FIELD;
        }

        default boolean isMethod() {
            return getTarget().kind() == Kind.METHOD;
        }

    }

}
