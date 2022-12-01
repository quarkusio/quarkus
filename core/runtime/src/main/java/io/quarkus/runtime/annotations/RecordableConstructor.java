package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that this constructor should be used to construct the recorded object.
 *
 * The constructor parameters will be read from the fields of the object, and matched
 * to the constructor parameter names.
 *
 * TODO: move this out of Quarkus core and into a tiny annotation-only module
 * that could then be used outside of Quarkus (for example in RESTEasy Reactive)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface RecordableConstructor {
}
