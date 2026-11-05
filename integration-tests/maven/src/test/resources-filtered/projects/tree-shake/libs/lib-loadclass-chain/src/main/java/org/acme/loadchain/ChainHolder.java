package org.acme.loadchain;

/**
 * Holds a member class that is only discovered by the class-loading chain analysis.
 * Nothing references {@code ChainHolder} itself, so it is only kept because
 * {@code ChainHolder$GammaTarget} is reachable and {@code Class.getSimpleName()} loads its enclosing class.
 */
public class ChainHolder {

    public static class GammaTarget {
    }
}
