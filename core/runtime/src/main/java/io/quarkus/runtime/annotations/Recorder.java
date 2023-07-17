package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the given type is a recorder that is used to record actions to be
 * executed at runtime.
 * <p>
 * Recorder classes must be non-final and have a public no-arg constructor.
 * <p>
 * At deployment time proxies of the recorder can be injected into BuildStep methods that
 * have been annotated with {@code @Record}. Any invocations made on these proxies will be
 * recorded, and bytecode will be written out to be executed at runtime to make the same
 * sequence of invocations with the same parameters on the actual recorder objects.
 *
 * Recorders can only have objects of the following types passed in:
 *
 * There are some limitations on what can be recorded. Only the following objects are allowed as parameters to
 * recording proxies:
 * <p>
 * - primitives
 * - String
 * - Class
 * - Objects returned from a previous recorder invocation
 * - Objects with a no-arg constructor and getter/setters for all properties (or public fields)
 * - Objects with a constructor annotated with @RecordableConstructor with parameter names that match field names
 * - Any arbitrary object via the
 * {@code io.quarkus.deployment.recording.RecorderContext#registerSubstitution(Class, Class, Class)} mechanism
 * - arrays, lists and maps of the above
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Recorder {
}
