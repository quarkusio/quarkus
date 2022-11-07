package io.quarkus.logging;

public interface LoggingInterface {
    default void inheritedMethod(String param) {
        if (Log.isInfoEnabled()) {
            Log.infof("Default method from interface: %s", param);
        }
    }
}
