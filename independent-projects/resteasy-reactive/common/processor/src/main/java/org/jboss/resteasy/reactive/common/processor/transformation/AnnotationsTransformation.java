package org.jboss.resteasy.reactive.common.processor.transformation;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.function.Predicate;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodParameterInfo;

/**
 * Represents a transformation of a collection of {@link AnnotationInstance} instances.
 */
public interface AnnotationsTransformation<T extends AnnotationsTransformation<T>> {

    /**
     * 
     * @param annotation
     * @return self
     */
    T add(AnnotationInstance annotation);

    /**
     * 
     * @param annotations
     * @return self
     */
    T addAll(Collection<AnnotationInstance> annotations);

    /**
     * 
     * @param annotations
     * @return self
     */
    T addAll(AnnotationInstance... annotations);

    /**
     * NOTE: The annotation target is derived from the transformation context. If you need to add an annotation instance
     * to a method parameter use methods consuming {@link AnnotationInstance} directly and supply the correct
     * {@link MethodParameterInfo}.
     * 
     * @param annotationType
     * @param values
     * @return self
     */
    T add(Class<? extends Annotation> annotationType, AnnotationValue... values);

    /**
     * NOTE: The annotation target is derived from the transformation context.. If you need to add an annotation instance
     * to a method parameter use methods consuming {@link AnnotationInstance} directly and supply the correct
     * {@link MethodParameterInfo}.
     * 
     * @param name
     * @param values
     * @return self
     */
    T add(DotName name, AnnotationValue... values);

    /**
     * Remove all annotations matching the given predicate.
     * 
     * @param predicate
     * @return self
     */
    T remove(Predicate<AnnotationInstance> predicate);

    /**
     * Remove all annotations.
     * 
     * @return self
     */
    T removeAll();

    /**
     * Applies the transformation.
     */
    void done();

}
