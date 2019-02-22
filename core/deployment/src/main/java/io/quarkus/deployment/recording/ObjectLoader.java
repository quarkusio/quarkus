package io.quarkus.deployment.recording;

import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.ResultHandle;

/**
 * A segment of code generation which produces the necessary instructions to load the given object. The result handle
 * is cached for reuse.
 */
public interface ObjectLoader {
    /**
     * Load the given object if possible.
     *
     * @param body the body to use for bytecode generation (not {@code null})
     * @param obj the object to substitute (not {@code null})
     * @return the result handle of the value, or {@code null} if this loader cannot load the given object
     */
    ResultHandle load(BytecodeCreator body, Object obj);
}
