package io.quarkus.test.common;

/**
 * Interface than can be implemented to notify the test infrastructure that the native image has started for
 * non HTTP based tests.
 *
 * This class has been deprecated and users should use {@link IntegrationTestStartedNotifier} instead when working
 * with {@code @QuarkusIntegrationTest}
 */
@Deprecated
public interface NativeImageStartedNotifier {

    boolean isNativeImageStarted();
}
