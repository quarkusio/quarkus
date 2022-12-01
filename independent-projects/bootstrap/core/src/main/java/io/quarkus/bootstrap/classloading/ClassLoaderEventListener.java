package io.quarkus.bootstrap.classloading;

/**
 * You can register classloader event listeners during our integration tests
 * to verify / count certain events.
 * This is useful, for example, to verify that certain classes or resources
 * are not being loaded; which in turn allows us to write regression tests
 * for certain optimisations.
 * Limitations: we don't intercept the system classloader, and the same operation
 * might be observed multiple times when multiple classloaders are chained.
 */
public interface ClassLoaderEventListener {

    default void enumeratingResourceURLs(String resourceName, String classLoaderName) {
    }

    default void gettingURLFromResource(String resourceName, String classLoaderName) {
    }

    default void openResourceStream(String resourceName, String classLoaderName) {
    }

    default void loadClass(String className, String classLoaderName) {
    }

}
