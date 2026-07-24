package io.quarkus.it.rest;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Quarkus omits nested classes from the generated JSON due to ignoreNested = true.
 * Mandrel/GraalVM reachability-metadata driven flow unconditionally registers
 * all nested classes, static, non-static, interfaces when the declaring class is registered.
 * The topic is called Complete Reflection Types and it is a future default of Mandrel/GraalVM.
 */
@RegisterForReflection(ignoreNested = true)
class ResourceA {

    private class InnerClassOfA {
    }

    protected static class StaticClassOfA {
    }

    private interface InterfaceOfA {
    }

}
