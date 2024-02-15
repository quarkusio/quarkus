package io.quarkus.it.rest;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Only Parent Class will be registered and none of the inner classes will be registered for reflection.
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
