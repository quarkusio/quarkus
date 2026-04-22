package org.acme.libreflection;

/**
 * This class has no direct bytecode reference from app code.
 * It is only reachable because it is registered for reflection via @RegisterForReflection.
 * Tree-shaking must preserve it and its transitive dependency (ReflectionDep).
 */
public class ReflectionTarget {
    private ReflectionDep dep;

    public String getName() {
        return "ReflectionTarget";
    }

    public ReflectionDep getDep() {
        return dep;
    }
}
