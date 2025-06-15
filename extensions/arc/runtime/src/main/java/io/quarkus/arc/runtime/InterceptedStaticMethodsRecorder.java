package io.quarkus.arc.runtime;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class InterceptedStaticMethodsRecorder {

    // This class is generated and calls all generated static interceptor initializers to register metadata in the
    // InterceptedStaticMethods class
    public static final String INITIALIZER_CLASS_NAME = "io.quarkus.arc.runtime.InterceptedStaticMethodsInitializer";

    public void callInitializer() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = InterceptedStaticMethodsRecorder.class.getClassLoader();
        }
        try {
            Class.forName(INITIALIZER_CLASS_NAME, true, cl);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

}
