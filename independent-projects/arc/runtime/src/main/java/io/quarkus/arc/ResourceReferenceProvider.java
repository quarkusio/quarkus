package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Makes it possible to resolve non-CDI injection points, such as Java EE resources.
 */
public interface ResourceReferenceProvider {

    /**
     * A resource reference handle is a dependent object of the object it is injected into. {@link InstanceHandle#destroy()} is
     * called when the target object is
     * destroyed.
     *
     * <pre>
     * class ResourceBean {
     *
     *     &#64;Resource(lookup = "bar")
     *     String bar;
     *
     *     &#64;Produces
     *     &#64;PersistenceContext
     *     EntityManager entityManager;
     * }
     * </pre>
     *
     * @param type
     * @param annotations
     * @return the resource reference handle or {@code null} if not resolvable
     */
    InstanceHandle<Object> get(Type type, Set<Annotation> annotations);

    /**
     * Convenient util method.
     *
     * @param annotations
     * @param annotationType
     * @return
     */
    @SuppressWarnings("unchecked")
    default <T extends Annotation> T getAnnotation(Set<Annotation> annotations, Class<T> annotationType) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(annotationType)) {
                return (T) annotation;
            }
        }
        return null;
    }

}
