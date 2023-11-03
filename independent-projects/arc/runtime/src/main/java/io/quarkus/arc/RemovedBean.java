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
     * @return the description of the declaring java member for producers
     */
    String getDescription();

    /**
     * @return the bean types
     */
    public Set<Type> getTypes();

    /**
     * @return the qualifiers
     */
    public Set<Annotation> getQualifiers();

}
