package io.quarkus.deployment.recording;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

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
     * @param staticInit {@code true} if this loader is for a static init method, {@code false} otherwise
     * @return the result handle of the value, or {@code null} if this loader cannot load the given object
     */
    ResultHandle load(BytecodeCreator body, Object obj, boolean staticInit);

    /**
     * Returns true if this object loader can handle the given object
     *
     * @param obj The object
     * @param staticInit If this is static init phase
     * @return true if this loader can handle the object
     */
    boolean canHandleObject(Object obj, boolean staticInit);
}
