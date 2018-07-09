package org.jboss.shamrock.core;

import java.io.IOException;

import org.jboss.shamrock.codegen.BytecodeRecorder;

/**
 * Interface that represents the current processor state. This is basically the output context, processors can use it
 * to generate bytecode or provide additional information.
 */
public interface ProcessorContext {

    /**
     * Adds a new deployment task with the given priority. This task will be run on startup in priority order.
     *
     * @param priority The priority
     * @return A recorder than can be used to generate bytecode
     */
    BytecodeRecorder addDeploymentTask(int priority);

    /**
     * This method is used to indicate that a given class requires reflection.
     *
     * It is used in the graal output to allow the class to be reflected when running on substrate VM
     *
     * @param className The class name
     */
    void addReflectiveClass(String className);

    void addGeneratedClass(String name, byte[] classData) throws IOException;

}
