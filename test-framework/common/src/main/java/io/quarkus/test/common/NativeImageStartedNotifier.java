package io.quarkus.test.common;

/**
 * Interface than can be implemented to notify the test infrastructure that the native image has started for
 * non HTTP based tests.
 */
public interface NativeImageStartedNotifier {

    boolean isNativeImageStarted();
}
