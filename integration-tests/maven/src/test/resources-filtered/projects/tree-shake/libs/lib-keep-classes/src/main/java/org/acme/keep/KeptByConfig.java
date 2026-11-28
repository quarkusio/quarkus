package org.acme.keep;

/**
 * Nothing references this class. It is kept only because it is listed in
 * {@code quarkus.package.jar.tree-shake.keep-classes}, which also keeps the classes it
 * references and its member classes.
 */
public class KeptByConfig {

    public String describe() {
        return new KeptByConfigDependency().toString();
    }

    public static class Member {
    }
}
