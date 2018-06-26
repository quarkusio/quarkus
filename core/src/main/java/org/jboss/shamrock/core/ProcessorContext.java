package org.jboss.shamrock.core;

import org.jboss.shamrock.codegen.BytecodeRecorder;

public interface ProcessorContext {

    BytecodeRecorder addDeploymentTask(int priority);

    void addReflectiveClass(String className);

}
