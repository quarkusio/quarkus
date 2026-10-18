package io.quarkus.arc.test.interceptors.bindings.transitive;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Documented
@SomeAnnotation
// contains annotation which is a binding, but is not a binding itself
public @interface NotABinding {
}
