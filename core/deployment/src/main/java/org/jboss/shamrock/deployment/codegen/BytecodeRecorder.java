package org.jboss.shamrock.deployment.codegen;

import java.io.IOException;

import org.jboss.shamrock.runtime.InjectionInstance;

public interface BytecodeRecorder extends AutoCloseable {

    InjectionInstance<?> newInstanceFactory(String className);

    <T> T getRecordingProxy(Class<T> theClass);

    Class<?> classProxy(String name);

    @Override
    void close() throws IOException;
}
