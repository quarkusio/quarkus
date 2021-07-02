package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * An unused bean removed during the build process.
 */
public interface RemovedBean {

    /**
     * @return the kind of the bean
     */
    InjectableBean.Kind getKind();

    /**
     * @return the description
     */
    String getDescription();

    /**
     * @return the bean types
     * @deprecated use {@link #types} to allow for future optimisations.
     */
    @Deprecated(forRemoval = true)
    Set<Type> getTypes();

    /**
     * @return the qualifiers
     * @deprecated use {@link #qualifiers} to allow for future optimisations.
     */
    @Deprecated(forRemoval = true)
    Set<Annotation> getQualifiers();

    /**
     * @param requiredType
     * @return if this is a match for the requiredType
     */
    boolean matchesType(Type requiredType);

    /**
     * Iterates on all qualifiers of this bean
     * 
     * @return
     */
    Iterable<Annotation> qualifiers();

    /**
     * Iterates on all types of this bean
     * 
     * @return
     */
    Iterable<Type> types();
}
