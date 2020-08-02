package io.quarkus.arc.runtime;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class InterceptedStaticMethodsRecorder {

    public static final String INTIALIZER_CLASS_NAME = "io.quarkus.arc.runtime.InterceptedStaticMethodsInitializer";

    public void callInitializer() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = InterceptedStaticMethodsRecorder.class.getClassLoader();
        }
        try {
            Class.forName(INTIALIZER_CLASS_NAME, true, cl);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

}
