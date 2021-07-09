package io.quarkus.test.common;

import java.nio.file.Path;

/**
 * Interface than can be implemented to notify the test infrastructure that the produced artifact has started for
 * non HTTP based {@code @QuarkusIntegrationTest}s.
 *
 * Implementations of this class are loaded via the ServiceLoader mechanism.
 */
public interface IntegrationTestStartedNotifier {

    /**
     * This method is called periodically by Quarkus to determine whether or not the application has started.
     * Quarkus will go through all the implementation of {@code IntegrationTestStartedNotifier} and call this method,
     * returning the first one that indicates a successful start, or {@link Result.NotStarted} otherwise
     */
    Result check(Context context);

    interface Context {
        Path logFile();
    }

    interface Result {
        boolean isStarted();

        boolean isSsl();

        final class NotStarted implements Result {

            public static final NotStarted INSTANCE = new NotStarted();

            private NotStarted() {
            }

            @Override
            public boolean isStarted() {
                return false;
            }

            @Override
            public boolean isSsl() {
                return false;
            }
        }
    }
}
