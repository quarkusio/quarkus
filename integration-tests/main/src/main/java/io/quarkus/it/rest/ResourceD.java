package io.quarkus.it.rest;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Registers target = ResourceC$InaccessibleClassOfC.
 * Mandrel/GraalVM reachability-metadata driven flow registering a nested class
 * retains its declaring class.
 */
@RegisterForReflection(classNames = "io.quarkus.it.rest.ResourceC$InaccessibleClassOfC")
public class ResourceD {

    public static class StaticClassOfD {

        public class OtherAccessibleClassOfD {
        }
    }
}
