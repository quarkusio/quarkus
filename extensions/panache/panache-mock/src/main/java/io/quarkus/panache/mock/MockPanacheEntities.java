package io.quarkus.panache.mock;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Install mocks for the given entity classes.
 * <p>
 * This annotation only works together with {@code QuarkusComponentTest}.
 *
 * @see PanacheMock#mock(Class...)
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface MockPanacheEntities {

    /**
     * The entity classes.
     */
    Class<?>[] value() default {};
}
