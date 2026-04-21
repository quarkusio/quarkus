package org.acme.multirelease;

/**
 * Referenced only by FutureVersionOnly (which lives in META-INF/versions/99/).
 * Must be preserved because multi-release classes for newer Java versions
 * participate in reachability analysis.
 */
public class FutureVersionDep {
    public String value() {
        return "dep";
    }
}
