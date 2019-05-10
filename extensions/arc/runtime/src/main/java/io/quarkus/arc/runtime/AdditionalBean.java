package io.quarkus.arc.runtime;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.inject.Stereotype;

/**
 * This built-in stereotype is automatically added to all additional beans.
 */
@Stereotype
@Target({ TYPE })
@Retention(RUNTIME)
public @interface AdditionalBean {

}
