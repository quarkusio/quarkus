package io.quarkus.runtime;

public interface IOThreadDetector {

    boolean isInIOThread();
}
