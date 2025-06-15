package io.quarkus.test.junit.mockito;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.InjectMock;

/**
 * This annotation can be used to configure a Mockito mock injected in a field of a test class that is annotated with
 * {@link InjectMock}. This annotation is only supported in a {@code io.quarkus.test.QuarkusTest}.
 *
 * @see InjectMock
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MockitoConfig {

    /**
     * If true, then Quarkus will change the scope of the target {@code Singleton} bean to {@code ApplicationScoped} to
     * make it mockable.
     * <p>
     * This is an advanced setting and should only be used if you don't rely on the differences between
     * {@code Singleton} and {@code ApplicationScoped} beans (for example it is invalid to read fields of
     * {@code ApplicationScoped} beans as a proxy stands in place of the actual implementation)
     */
    boolean convertScopes() default false;

    /**
     * If true, the mock will be created with the {@link org.mockito.Mockito#RETURNS_DEEP_STUBS}
     */
    boolean returnsDeepMocks() default false;
}
