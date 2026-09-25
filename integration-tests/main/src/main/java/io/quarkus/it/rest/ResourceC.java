package io.quarkus.it.rest;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Registers target = ResourceD.StaticClassOfD.
 * Quarkus omits nested classes of the target from the JSON as `ignoreNested = true`.
 * Mandrel/GraalVM reachability-metadata driven flow unconditionally registers
 * the target's nested classes e.g. OtherAccessibleClassOfD and its declaring class ResourceD.
 */
@RegisterForReflection(targets = ResourceD.StaticClassOfD.class, ignoreNested = true)
public class ResourceC {

    private class InaccessibleClassOfC {

        public class OtherInaccessibleClassOfC {
        }
    }
}
