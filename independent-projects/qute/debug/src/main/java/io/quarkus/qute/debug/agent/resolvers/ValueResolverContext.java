package io.quarkus.qute.debug.agent.resolvers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.quarkus.qute.debug.agent.frames.RemoteStackFrame;

/**
 * Context used by the debugger to collect properties and methods of an object
 * in a given stack frame.
 * <p>
 * Implementations of this interface are used in different scenarios:
 * <ul>
 * <li>{@link io.quarkus.qute.debug.agent.completions.CompletionContext} for code
 * completion, where both methods and properties are collected as completion items.</li>
 * <li>{@link io.quarkus.qute.debug.agent.variables.VariableContext} for variable
 * inspection, where properties are collected as debug variables and methods
 * may be ignored.</li>
 * </ul>
 * </p>
 * <p>
 * The context provides access to the base object and the current stack frame, and
 * allows adding methods or properties dynamically. It also allows the implementation
 * to indicate whether to collect methods or properties via
 * {@link #isCollectProperty()} and {@link #isCollectMethod()}.
 * </p>
 */
public interface ValueResolverContext {

    /** Returns the base object being inspected. */
    Object getBase();

    /** Returns the current stack frame for the debugger. */
    RemoteStackFrame getStackFrame();

    /** Registers a method to the context (for completion or variable tracking). */
    void addMethod(Method method);

    /** Registers a field as a property in the context. */
    void addProperty(Field field);

    /** Registers a property by name. */
    void addProperty(String property);

    /** Registers a method by name. */
    void addMethod(String method);

    /** Returns true if the context is collecting properties. */
    boolean isCollectProperty();

    /** Returns true if the context is collecting methods. */
    boolean isCollectMethod();
}
