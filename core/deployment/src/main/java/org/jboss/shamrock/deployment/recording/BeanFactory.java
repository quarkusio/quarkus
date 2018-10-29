package org.jboss.shamrock.deployment.recording;

import org.jboss.shamrock.runtime.InjectionInstance;

/**
 * TODO: get rid of this
 */
public class BeanFactory {

    private final BytecodeRecorderImpl bytecodeRecorder;

    public BeanFactory(BytecodeRecorderImpl bytecodeRecorder) {
        this.bytecodeRecorder = bytecodeRecorder;
    }

    public InjectionInstance<?> newInstanceFactory(String className) {
        return bytecodeRecorder.newInstanceFactory(className);
    }
}
