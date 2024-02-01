package io.quarkus.it.rest;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * This class is registering targets = ResourceC$InaccessibleClassOfC
 * The class itself won't be registered by this, only target will be registered including target's nested classes
 */
@RegisterForReflection(classNames = "io.quarkus.it.rest.ResourceC$InaccessibleClassOfC")
public class ResourceD {

    // Parent class won't be registered, only the below private class will be registered without nested classes
    public static class StaticClassOfD {

        public class OtherAccessibleClassOfD {
        }
    }
}