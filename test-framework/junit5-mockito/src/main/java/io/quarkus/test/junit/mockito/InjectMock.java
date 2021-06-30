package io.quarkus.test.junit.mockito;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When used on a field of a test class, the field becomes a Mockito mock,
 * that is then used to mock the normal scoped bean which the field represents
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectMock {

    /**
     * If true, then Quarkus will change the scope of the target {@code Singleton} bean to {@code ApplicationScoped}
     * to make the mockable.
     * This is an advanced setting and should only be used if you don't rely on the differences between {@code Singleton}
     * and {@code ApplicationScoped} beans (for example it is invalid to read fields of {@code ApplicationScoped} beans
     * as a proxy stands in place of the actual implementation)
     */
    boolean convertScopes() default false;

    /**
     * If true, the mock will be created with the {@link org.mockito.Mockito#RETURNS_DEEP_STUBS}
     */
    boolean returnsDeepMocks() default false;
}
