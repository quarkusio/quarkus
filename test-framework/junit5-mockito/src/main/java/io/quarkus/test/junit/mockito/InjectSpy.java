package io.quarkus.test.junit.mockito;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When used on a field of a test class, the field becomes a Mockito spy,
 * that is then used to spy on the normal scoped bean which the field represents
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectSpy {
}
