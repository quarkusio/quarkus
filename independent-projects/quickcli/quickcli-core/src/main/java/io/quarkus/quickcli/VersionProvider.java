package io.quarkus.quickcli;

/**
 * Provides version information dynamically.
 */
public interface VersionProvider {

    /**
     * Returns version strings to display when --version is requested.
     */
    String[] getVersion() throws Exception;

    /**
     * Sentinel class indicating no version provider is configured.
     */
    final class NoVersionProvider implements VersionProvider {
        @Override
        public String[] getVersion() {
            return new String[0];
        }
    }
}
