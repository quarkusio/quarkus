package io.quarkus.arc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.enterprise.context.NormalScope;

/**
 * Indicates that a given object has a managed scope. This means that whenever
 * the object is invoked upon the relevant producer method will be called to return
 * the correct object.
 *
 * Note that managed objects do not support disposal methods.
 */
@NormalScope
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface Managed {
}
