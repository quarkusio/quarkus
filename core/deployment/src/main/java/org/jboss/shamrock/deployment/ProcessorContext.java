package org.jboss.shamrock.deployment;

import java.io.IOException;

import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;

/**
 * Interface that represents the current processor state. This is basically the output context, processors can use it
 * to generate bytecode or provide additional information.
 */
public interface ProcessorContext {


    /**
     * Adds a new static init task with the given priority. This task will be from a static init
     * block in priority order
     *
     * These tasks are always run before deployment tasks
     *
     * @param priority The priority
     * @return A recorder than can be used to generate bytecode
     */
    BytecodeRecorder addStaticInitTask(int priority);

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
