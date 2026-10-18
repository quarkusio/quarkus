package org.acme.libreflection;

/**
 * Not registered for reflection and not referenced by any reachable code.
 * Tree-shaking should remove this class.
 */
public class UnusedReflection {
    public String neverCalled() {
        return "unused";
    }
}
