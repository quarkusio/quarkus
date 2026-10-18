package org.acme.libreflection;

/**
 * Transitive dependency of ReflectionTarget.
 * Must be preserved because ReflectionTarget (a reflection root) references it.
 */
public class ReflectionDep {
    public String getValue() {
        return "ReflectionDep";
    }
}
