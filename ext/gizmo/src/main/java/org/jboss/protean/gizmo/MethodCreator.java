package org.jboss.protean.gizmo;

import java.util.List;

/**
 * A class that builds the body of a method without needing to understand java bytecode.
 */
public interface MethodCreator extends MemberCreator<MethodCreator>, BytecodeCreator, AnnotatedElement {

    /**
     * Adds an exception to the method signature
     *
     * @param exception The exception
     *
     * @return This creator
     */
    MethodCreator addException(String exception);

    /**
     * Adds an exception to the method signature
     *
     * @param exception The exception
     *
     * @return This creator
     */
    default MethodCreator addException(Class<?> exception) {
        return addException(exception.getName());
    }

    /**
     *
     * @return The exceptions thrown by this method
     */
    List<String> getExceptions();

    /**
     *
     * @return The method descriptor
     */
    MethodDescriptor getMethodDescriptor();

    AnnotatedElement getParameterAnnotations(int param);

}
