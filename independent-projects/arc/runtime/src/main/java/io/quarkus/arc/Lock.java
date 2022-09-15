package io.quarkus.arc;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Defines a concurrency lock for a bean.
 * <p>
 * The container provides a built-in interceptor for this interceptor binding. Each interceptor instance associated with a
 * contextual instance of an intercepted bean holds a {@link ReadWriteLock} instance with non-fair ordering policy.
 */
@InterceptorBinding
@Inherited
@Target(value = { TYPE, METHOD })
@Retention(value = RUNTIME)
public @interface Lock {

    /**
     *
     * @return the type of the lock
     */
    @Nonbinding
    Type value() default Type.WRITE;

    /**
     * If it's not possible to acquire the lock in the given time a {@link LockException} is thrown.
     *
     * @see java.util.concurrent.locks.Lock#tryLock(long, TimeUnit)
     * @return the wait time
     */
    @Nonbinding
    long time() default -1l;

    /**
     *
     * @return the wait time unit
     */
    @Nonbinding
    TimeUnit unit() default TimeUnit.MILLISECONDS;

    public enum Type {
        /**
         * Acquires the read lock before the business method is invoked.
         */
        READ,
        /**
         * Acquires the write (exclusive) lock before the business method is invoked.
         */
        WRITE,
        /**
         * Acquires no lock.
         * <p>
         * This could be useful if you need to override the behavior defined by a class-level interceptor binding.
         */
        NONE
    }

}
