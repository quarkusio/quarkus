package io.quarkus.bootstrap.classloading;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ClassLoaderLimiter implements ClassLoaderEventListener {

    //Store which classloader (by name) has loaded each resource as it helps diagnostics
    private final ConcurrentMap<String, String> atMostOnceResourcesLoaded = new ConcurrentHashMap();
    private final ConcurrentMap<String, String> allResourcesLoaded = new ConcurrentHashMap();

    private final Set<String> vetoedResources;
    private final Set<String> vetoedClasses;
    private final Set<String> vetoedRuntimeClasses;
    private final Set<String> atMostOnceResources;
    private final Set<String> onHitPrintStacktrace;
    private final boolean traceAllResourceLoad;

    private ClassLoaderLimiter(Builder builder) {
        vetoedClasses = builder.vetoedClasses;
        vetoedResources = builder.vetoedResources;
        vetoedRuntimeClasses = builder.vetoedRuntimeClasses;
        atMostOnceResources = builder.atMostOnceResources;
        onHitPrintStacktrace = builder.onHitPrintStacktrace;
        traceAllResourceLoad = builder.traceAllResourceLoad;
    }

    @Override
    public void openResourceStream(String resourceName, String classLoaderName) {
        Objects.requireNonNull(resourceName);
        Objects.requireNonNull(classLoaderName);
        if (traceAllResourceLoad) {
            System.out.println("Opening resource: " + resourceName);
        }
        if (onHitPrintStacktrace.contains(resourceName)) {
            final RuntimeException e = new RuntimeException("Tracing load of resource: " + resourceName);
            e.printStackTrace();
        }
        if (vetoedResources.contains(resourceName)) {
            throw new IllegalStateException(
                    "Attempted to load vetoed resource '" + resourceName + "' from classloader " + classLoaderName);
        }
        if (atMostOnceResources.contains(resourceName)) {
            final String previousLoadEvent = atMostOnceResourcesLoaded.put(resourceName, classLoaderName);
            if (previousLoadEvent != null) {
                throw new IllegalStateException("Resource being loaded more than once: " + resourceName + ".\n" +
                        "Attempted load by " + classLoaderName + ", recorded previous load by " + previousLoadEvent);
            }
        }
        if (resourceName.endsWith(".class")) {
            //Skip further tracking on classes as it would create unnecessary noise
            return;
        }
        final String previousLoad = allResourcesLoaded.put(resourceName, classLoaderName);
        if (previousLoad != null) {
            //This diagnostic has no flag, as it's generally useful, doesn't throw exceptions, and should
            //generally not log much at all.
            System.out.println(
                    "Resource loaded multiple times: " + resourceName + ". Currently being loaded by " + classLoaderName +
                            ", previous loaded by " + previousLoad);
        }
    }

    @Override
    public void loadClass(String className, String classLoaderName) {
        if (vetoedClasses.contains(className)) {
            throw new IllegalStateException(
                    "Attempted to load vetoed class '" + className + "' from classloader " + classLoaderName);
        } else if (vetoedRuntimeClasses.contains(className) && classLoaderName.toLowerCase(Locale.ROOT).contains("runtime")) {
            throw new IllegalStateException(
                    "Attempted to load vetoed class '" + className + "' from classloader " + classLoaderName);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Set<String> vetoedResources = new TreeSet<>();
        private final Set<String> vetoedClasses = new TreeSet<>();
        private final Set<String> vetoedRuntimeClasses = new TreeSet<>();
        private final Set<String> atMostOnceResources = new TreeSet<>();
        private final Set<String> onHitPrintStacktrace = new TreeSet<>();
        private boolean traceAllResourceLoad = false;

        private Builder() {
            //private constructor to encourage using the builder() method.
        }

        /**
         * List a resource name as one that you don't expect to be loaded ever.
         * If there is an attempt of loading the matched resource, a runtime exception will be thrown instead:
         * useful for running integration tests to verify your assumptions.
         *
         * Limitations: if the resource is being loaded using the bootstrap classloader we
         * can't check it; some frameworks explicitly request using the base classloader
         * for resource loading (or even use the Filesystem API), so they can't be tested via this method.
         *
         * @param resourceFullName the resource name
         * @return this, for method chaining.
         */
        public Builder neverLoadedResource(String resourceFullName) {
            Objects.requireNonNull(resourceFullName);
            final boolean add = vetoedResources.add(resourceFullName);
            if (!add) {
                throw new ClassLoaderLimiterConsistencyException(
                        "resource listed multiple times as never loaded: " + resourceFullName);
            }
            if (atMostOnceResources.contains(resourceFullName)) {
                throw new ClassLoaderLimiterConsistencyException(
                        resourceFullName + " is being listed both as never loaded and as at most once");
            }
            return this;
        }

        /**
         * List a fully qualified class name as one that you don't expect to be loaded ever.
         * If there is an attempt of loading the matched class, a runtime exception will be thrown instead:
         * useful for running integration tests to verify your assumptions.
         *
         * DO NOT list the name by doing using <code>literal.class.getName()</code> as this will implicitly get you
         * to load the class during the test, and produce a failure.
         *
         * Limitations: if the class is being loaded using the bootstrap classloader we
         * can't check it. Most Quarkus extensions and frameworks will not use the bootstrap classloader,
         * but some code could make use of it explicitly.
         *
         * @param vetoedClassName the fully qualified class name
         * @return this, for method chaining.
         */
        public Builder neverLoadedClassName(String vetoedClassName) {
            Objects.requireNonNull(vetoedClassName);
            final boolean add = vetoedClasses.add(vetoedClassName);
            if (!add)
                throw new ClassLoaderLimiterConsistencyException(
                        "never loaded class listed multiple times: " + vetoedClassName);
            return this;
        }

        /**
         * List a fully qualified class name as one that you don't expect to be loaded at runtime.
         * If there is an attempt of loading the matched class, a runtime exception will be thrown instead:
         * useful for running integration tests to verify your assumptions.
         *
         * DO NOT list the name by doing using <code>literal.class.getName()</code> as this will implicitly get you
         * to load the class during the test, and produce a failure.
         *
         * Limitations: if the class is being loaded using the bootstrap classloader we
         * can't check it. Most Quarkus extensions and frameworks will not use the bootstrap classloader,
         * but some code could make use of it explicitly.
         *
         * @param vetoedClassName the fully qualified class name
         * @return this, for method chaining.
         */
        public Builder neverLoadedRuntimeClassName(String vetoedClassName) {
            Objects.requireNonNull(vetoedClassName);
            final boolean add = vetoedRuntimeClasses.add(vetoedClassName);
            if (!add)
                throw new ClassLoaderLimiterConsistencyException(
                        "never loaded class listed multiple times: " + vetoedClassName);
            return this;
        }

        /**
         * Useful to check that a resource is being loaded only once, or never.
         * If there is an attempt of loading the matched resource more than once, a runtime exception will be thrown instead:
         * useful for running integration tests to verify your assumptions.
         *
         * Limitations: if the resource is being loaded using the bootstrap classloader we
         * can't check it; some frameworks explicitly request using the base classloader
         * for resource loading (or even use the Filesystem API), so they can't be tested via this method.
         * Additionally, some frameworks will load the same resource but in different Quarkus build
         * phases (which map to different classloaders); such cases will count as multiple times
         * so it might not be suited to test this way.
         *
         * @param resourceFullName the resource name
         * @return this, for method chaining.
         */
        public Builder loadedAtMostOnceResource(String resourceFullName) {
            Objects.requireNonNull(resourceFullName);
            final boolean add = atMostOnceResources.add(resourceFullName);
            if (!add)
                throw new ClassLoaderLimiterConsistencyException(
                        "resource listed multiple times as loaded at most once: " + resourceFullName);
            if (vetoedResources.contains(resourceFullName)) {
                throw new ClassLoaderLimiterConsistencyException(
                        resourceFullName + " is being listed both as never loaded and as at most once");
            }
            return this;
        }

        /**
         * When the specified resource is loaded, print a full stack trace on System out.
         * This is not useful for testing, but handy to trace were a certain load is coming from,
         * should you need to diagnose a failing expectation.
         *
         * @param resourceFullName
         * @return this, for method chaining.
         */
        public Builder produceStackTraceOnLoad(String resourceFullName) {
            Objects.requireNonNull(resourceFullName);
            final boolean add = onHitPrintStacktrace.add(resourceFullName);
            if (!add)
                throw new ClassLoaderLimiterConsistencyException(
                        "resource listed multiple times to produce a stacktrace: " + resourceFullName);
            return this;
        }

        /**
         * Simply log all resource loading events. Useful to get an idea of which resources are being loaded;
         * Note it includes resources being used to load classes.
         *
         * @param enable
         * @return this, for method chaining.
         */
        public Builder traceAllResourceLoad(boolean enable) {
            traceAllResourceLoad = enable;
            return this;
        }

        public ClassLoaderLimiter build() {
            return new ClassLoaderLimiter(this);
        }

    }

    public static class ClassLoaderLimiterConsistencyException extends IllegalArgumentException {
        public ClassLoaderLimiterConsistencyException(String detail) {
            super("ClassLoaderLimiter definition inconsistency: " + detail);
        }
    }

}
