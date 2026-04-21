package org.acme.libjni;

/**
 * Transitive dependency of {@link JniTarget}. Should be preserved because
 * JniTarget references it and JniTarget is a tree-shake root.
 */
public class JniDep {
    public String getValue() {
        return "JniDep";
    }
}
