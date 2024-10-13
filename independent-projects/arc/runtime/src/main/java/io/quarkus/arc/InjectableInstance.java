package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.util.TypeLiteral;

/**
 * Enhanced version of {@link Instance}.
 *
 * @param <T>
 */
public interface InjectableInstance<T> extends Instance<T> {

    @Override
    InstanceHandle<T> getHandle();

    @Override
    Iterable<InstanceHandle<T>> handles();

    @Override
    default Stream<InstanceHandle<T>> handlesStream() {
        // copy of `Instance.handlesStream()` to avoid unchecked conversion
        return StreamSupport.stream(handles().spliterator(), false);
    }

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
     * @return the bean instance or {@code null}
     */
    default T orNull() {
        return orElse(null);
    }

    /**
     * Returns exactly one instance of an {@linkplain InjectableBean#checkActive() active} bean that matches
     * the required type and qualifiers. If no active bean matches, or if more than one active bean matches,
     * throws an exception.
     *
     * @return the single instance of an active matching bean
     */
    default T getActive() {
        List<T> list = listActive();
        if (list.size() == 1) {
            return list.get(0);
        } else if (list.isEmpty()) {
            throw new UnsatisfiedResolutionException("No active bean found");
        } else {
            throw new AmbiguousResolutionException("More than one active bean found: " + list);
        }
    }

    /**
     * Returns the list of instances of {@linkplain InjectableBean#checkActive() active} beans that match
     * the required type and qualifiers, sorter in priority order (higher priority goes first).
     *
     * @return the list of instances of matching active beans
     */
    default List<T> listActive() {
        return handlesStream().filter(it -> it.getBean().isActive()).map(InstanceHandle::get).toList();
    }
}
