package io.quarkus.arc.runtime;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Stereotype;

/**
 * This built-in stereotype is automatically added to all additional beans that do not have a scope annotation declared.
 * <p>
 * Note that stereotypes are bean defining annotations and so bean classes annotated with this stereotype but no scope have
 * their scope defaulted to {@link Dependent}.
 */
@Stereotype
@Target({ TYPE })
@Retention(RUNTIME)
public @interface AdditionalBean {

}
