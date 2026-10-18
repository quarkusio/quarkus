package io.quarkus.qute;

public interface WithPriority {

    int DEFAULT_PRIORITY = 1;

    /**
     * @return the priority value
     */
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }

}