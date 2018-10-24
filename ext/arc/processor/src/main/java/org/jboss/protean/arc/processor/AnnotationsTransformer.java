package org.jboss.protean.arc.processor;

import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;

/**
 * Allows a build-time extension to override the annotations that exist on bean classes.
 *
 * @author Martin Kouba
 */
public interface AnnotationsTransformer extends BuildProcessor {

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
     * @param target
     * @param annotations
     * @return the transformed annotations
     * @see Transformation
     */
    Collection<AnnotationInstance> transform(AnnotationTarget target, Collection<AnnotationInstance> annotations);

}
