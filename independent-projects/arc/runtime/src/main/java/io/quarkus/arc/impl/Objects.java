package io.quarkus.arc.impl;

/**
 * @deprecated will be removed. Use Gizmo's ifReferencesEqual instead.
 */
@Deprecated
public final class Objects {

    private Objects() {
    }

    // https://github.com/quarkusio/gizmo/issues/50
    public static boolean referenceEquals(Object obj1, Object obj2) {
        return obj1 == obj2;
    }
}
