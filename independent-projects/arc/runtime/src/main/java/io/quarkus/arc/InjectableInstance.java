package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

/**
 * Enhanced version of {@link Instance}.
 *
 * @param <T>
 */
public interface InjectableInstance<T> extends Instance<T> {

    InstanceHandle<T> getHandle();

    Iterable<InstanceHandle<T>> handles();

    @Override
    InjectableInstance<T> select(Annotation... qualifiers);

    @Override
    <U extends T> InjectableInstance<U> select(Class<U> subtype, Annotation... qualifiers);

    @Override
    <U extends T> InjectableInstance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers);

    /**
     * Removes the cached result of the {@link #get()} operation. If the cached result was a contextual reference of
     * a {@link Dependent} bean, destroy the reference as well.
     *
     * @see WithCaching
     */
    void clearCache();

    /**
     * This method attempts to resolve ambiguities.
     * <p>
     * In general, if multiple beans are eligible then the container eliminates all beans that are:
     * <ul>
     * <li>not alternatives, except for producer methods and fields of beans that are alternatives,</li>
     * <li>default beans.</li>
     * </ul>
     *
     * @return an iterator over the contextual references of the disambiguated beans
     * @see DefaultBean
     */
    @Override
    Iterator<T> iterator();

    /**
     * If there is exactly one bean that matches the required type and qualifiers, returns the instance, otherwise returns
     * {@code other}.
     *
     * @param other
     * @return the bean instance or the other value
     */
    default T orElse(T other) {
        return isResolvable() ? get() : other;
    }

    /**
     * If there is exactly one bean that matches the required type and qualifiers, returns the instance, otherwise returns
     * {@code null}.
     *
     * @param other
     * @return the bean instance or {@code null}
     */
    default T orNull() {
        return orElse(null);
    }

}
