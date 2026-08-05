package org.acme.libjni;

/**
 * Not registered for JNI access and not referenced by any reachable code.
 * Should be removed by tree-shaking.
 */
public class UnusedJni {
    public String neverCalled() {
        return "unused";
    }
}
