package io.quarkus.it.rest;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Both Parent Class and all nested classed will be registered for reflection
 */
@RegisterForReflection(ignoreNested = false)
public class ResourceB {

    private class InnerClassOfB {

        private class InnerInnerOfB {
        }
    }

    protected static class StaticClassOfB {
    }

    private interface InterfaceOfB {
    }

}