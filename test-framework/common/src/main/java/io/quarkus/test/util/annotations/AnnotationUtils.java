package io.quarkus.test.util.annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.platform.commons.util.Preconditions;

/**
 * Provides utility methods for obtaining annotations on test classes. This class is basically an adaptation of
 * {@link org.junit.platform.commons.support.AnnotationSupport} altered to include the element which was annotated in
 * the result and filtered out to only contain methods we use.
 */
public final class AnnotationUtils {

    private AnnotationUtils() {
    }

    /**
     * Find the first annotation of {@code annotationType} that is either <em>directly present</em>,
     * <em>meta-present</em>, or <em>indirectly present</em> on the supplied {@code element}.
     * <p>
     * If the element is a class and the annotation is neither <em>directly present</em> nor <em>meta-present</em> on
     * the class, this method will additionally search on interfaces implemented by the class before finding an
     * annotation that is <em>indirectly present</em> on the class (meaning that the same process will be repeated for
     * superclasses if {@link Inherited} is present on {@code annotationType}).
     *
     * @param <A>
     *        the annotation type
     * @param element
     *        the element on which to search for the annotation; may be {@code null}
     * @param annotationType
     *        the annotation type to search for; never {@code null}
     *
     * @return an {@code Optional} containing the annotation and the element on which it was present; never {@code null}
     *         but potentially empty
     */
    public static <A extends Annotation> Optional<AnnotationContainer<A>> findAnnotation(AnnotatedElement element,
            Class<A> annotationType) {
        Preconditions.notNull(annotationType, "annotationType must not be null");
        boolean inherited = annotationType.isAnnotationPresent(Inherited.class);
        return findAnnotation(element, annotationType, inherited, new HashSet<>());
    }

    private static <A extends Annotation> Optional<AnnotationContainer<A>> findAnnotation(AnnotatedElement element,
            Class<A> annotationType, boolean inherited, Set<Annotation> visited) {

        Preconditions.notNull(annotationType, "annotationType must not be null");

        if (element == null) {
            return Optional.empty();
        }

        // Directly present?
        A annotation = element.getDeclaredAnnotation(annotationType);
        if (annotation != null) {
            return Optional.of(new AnnotationContainer<>(element, annotation));
        }

        // Meta-present on directly present annotations?
        Optional<AnnotationContainer<A>> directMetaAnnotation = findMetaAnnotation(annotationType,
                element.getDeclaredAnnotations(), inherited, visited);
        if (directMetaAnnotation.isPresent()) {
            return directMetaAnnotation;
        }

        if (element instanceof Class) {
            Class<?> clazz = (Class<?>) element;

            // Search on interfaces
            for (Class<?> ifc : clazz.getInterfaces()) {
                if (ifc != Annotation.class) {
                    Optional<AnnotationContainer<A>> annotationOnInterface = findAnnotation(ifc, annotationType,
                            inherited, visited);
                    if (annotationOnInterface.isPresent()) {
                        return annotationOnInterface;
                    }
                }
            }

            // Indirectly present?
            // Search in class hierarchy
            if (inherited) {
                Class<?> superclass = clazz.getSuperclass();
                if (superclass != null && superclass != Object.class) {
                    Optional<AnnotationContainer<A>> annotationOnSuperclass = findAnnotation(superclass, annotationType,
                            inherited, visited);
                    if (annotationOnSuperclass.isPresent()) {
                        return annotationOnSuperclass;
                    }
                }
            }
        }

        // Meta-present on indirectly present annotations?
        return findMetaAnnotation(annotationType, element.getAnnotations(), inherited, visited);
    }

    private static <A extends Annotation> Optional<AnnotationContainer<A>> findMetaAnnotation(Class<A> annotationType,
            Annotation[] candidates, boolean inherited, Set<Annotation> visited) {

        for (Annotation candidateAnnotation : candidates) {
            Class<? extends Annotation> candidateAnnotationType = candidateAnnotation.annotationType();
            if (!isInJavaLangAnnotationPackage(candidateAnnotationType) && visited.add(candidateAnnotation)) {
                Optional<AnnotationContainer<A>> metaAnnotation = findAnnotation(candidateAnnotationType,
                        annotationType, inherited, visited);
                if (metaAnnotation.isPresent()) {
                    return metaAnnotation;
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isInJavaLangAnnotationPackage(Class<? extends Annotation> annotationType) {
        return (annotationType != null && annotationType.getName().startsWith("java.lang.annotation"));
    }
}
