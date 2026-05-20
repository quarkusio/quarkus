package io.quarkus.it.nat.annotation;

import java.lang.invoke.SerializedLambda;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.runtime.annotations.RegisterResourceBundle;
import io.quarkus.runtime.annotations.RegisterResources;

/*
Lambda serialization mechanism needs both
SerializedLambda AND the types it uses internally
all registered.
*/
@RegisterForReflection(targets = {
        SerializedLambda.class,
        String.class,
        Object[].class
}, serialization = true)
/*
 * Tests registering a resource, e.g. a file.
 */
@RegisterResources(globs = "file.txt")
/*
 * Tests registering resource bundles.
 */
@RegisterResourceBundle(bundleName = "messages")
public class ProperHints {
}
