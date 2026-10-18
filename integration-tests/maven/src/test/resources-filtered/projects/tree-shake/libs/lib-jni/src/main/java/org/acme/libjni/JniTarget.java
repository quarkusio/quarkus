package org.acme.libjni;

/**
 * Class registered as a JNI runtime access root via {@code JniRuntimeAccessBuildItem}.
 * Not referenced from any bytecode — only reachable because the build step registers it.
 */
public class JniTarget {
    private JniDep dep;

    public String getName() {
        return "JniTarget";
    }

    public JniDep getDep() {
        return dep;
    }
}
