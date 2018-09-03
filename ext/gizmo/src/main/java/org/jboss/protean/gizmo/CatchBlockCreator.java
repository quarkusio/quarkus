package org.jboss.protean.gizmo;

/**
 * A bytecode creator that represents a catch block
 */
public interface CatchBlockCreator extends BytecodeCreator {

    /**
     *
     * @return A result handle representing the caught exception
     */
    ResultHandle getCaughtException();

}
