package io.quarkus.it.rest;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * This class is registering targets = ResourceD.StaticClassOfD
 * The class itself won't be registered by this, only target will be registered without registering nested classes
 */
@RegisterForReflection(targets = ResourceD.StaticClassOfD.class, ignoreNested = true)
public class ResourceC {

    private class InaccessibleClassOfC {

        public class OtherInaccessibleClassOfC {
        }
    }
}