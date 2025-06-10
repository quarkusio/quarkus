package io.quarkus.test.junit;

import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.TestResourceScope;

/**
 * Contains methods that are needed for determining how to deal with {@link io.quarkus.test.common.QuarkusTestResource} and
 * {@link io.quarkus.test.common.WithTestResource}
 */
public final class TestResourceUtil {

    private TestResourceUtil() {
    }

    /**
     * This is where we decide if the test resources of the current state vs the ones required by the next test class
     * to be executed require a Quarkus restart.
     */
    public static boolean testResourcesRequireReload(QuarkusTestExtensionState state, Class<?> nextTestClass,
            Class<? extends QuarkusTestProfile> nextTestClassProfile) {
        QuarkusTestProfile profileInstance = instantiateProfile(nextTestClassProfile);
        Set<TestResourceManager.TestResourceComparisonInfo> existingTestResources = existingTestResources(state);
        Set<TestResourceManager.TestResourceComparisonInfo> nextTestResources = nextTestResources(nextTestClass,
                profileInstance);

        return TestResourceManager.testResourcesRequireReload(existingTestResources, nextTestResources);
    }

    static Set<TestResourceManager.TestResourceComparisonInfo> existingTestResources(QuarkusTestExtensionState state) {
        if (state == null) {
            return Collections.emptySet();
        }
        Closeable closeable = state.testResourceManager;
        if (closeable == null) {
            return Collections.emptySet();
        }
        // we can't compare with instanceof because of the different CL
        if (TestResourceManager.class.getName().equals(closeable.getClass().getName())) {
            return TestResourceManagerReflections
                    .testResourceComparisonInfo(closeable);
        }
        return Collections.emptySet();
    }

    static Set<TestResourceManager.TestResourceComparisonInfo> nextTestResources(Class<?> requiredTestClass,
            QuarkusTestProfile profileInstance) {

        List<TestResourceManager.TestResourceClassEntry> entriesFromProfile = Collections.emptyList();
        if (profileInstance != null) {
            entriesFromProfile = new ArrayList<>(profileInstance.testResources().size());
            for (QuarkusTestProfile.TestResourceEntry entry : profileInstance.testResources()) {
                entriesFromProfile.add(new TestResourceManager.TestResourceClassEntry(entry.getClazz(), entry.getArgs(), null,
                        entry.isParallel(), TestResourceScope.MATCHING_RESOURCES));
            }
        }

        return TestResourceManager
                .testResourceComparisonInfo(requiredTestClass, getTestClassesLocation(requiredTestClass), entriesFromProfile);
    }

    public static String getReloadGroupIdentifier(Class<?> requiredTestClass,
            Class<? extends QuarkusTestProfile> profileClass) {
        return TestResourceManager
                .getReloadGroupIdentifier(nextTestResources(requiredTestClass, instantiateProfile(profileClass)));
    }

    private static QuarkusTestProfile instantiateProfile(Class<? extends QuarkusTestProfile> nextTestClassProfile) {
        if (nextTestClassProfile != null) {
            // The class we are given could be in the app classloader, so swap it over
            // All this reflective classloading is a bit wasteful, so it would be ideal if the implementation was less picky about classloaders (that's not just moving the reflection further down the line)
            // TODO this may not be necessary with changes in FacadeClassLoader, check
            // TODO not only is it not necessary, it actually cannot work because we've lost access to the runtime classloader which we need to load app classes
            try {
                if (!QuarkusTestProfile.class.isAssignableFrom(nextTestClassProfile)) {
                    nextTestClassProfile = (Class<? extends QuarkusTestProfile>) TestResourceUtil.class.getClassLoader()
                            .loadClass(nextTestClassProfile.getName());
                }
                return nextTestClassProfile.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    /**
     * Contains a bunch of utilities that are needed for handling {@link TestResourceManager}
     * via reflection (due to different classloaders)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final class TestResourceManagerReflections {

        private TestResourceManagerReflections() {
        }

        /**
         * Since {@link TestResourceManager} is loaded from the ClassLoader passed in as an argument,
         * we need to convert the user input {@link QuarkusTestProfile.TestResourceEntry} into instances of
         * {@link TestResourceManager.TestResourceClassEntry} that are loaded from that ClassLoader
         */
        public static <T> List<T> copyEntriesFromProfile(
                QuarkusTestProfile profileInstance, ClassLoader classLoader) {
            if ((profileInstance == null) || profileInstance.testResources()
                    .isEmpty()) {
                return Collections.emptyList();
            }

            try {
                Class testResourceScopeClass = classLoader.loadClass(TestResourceScope.class.getName());
                Constructor<?> testResourceClassEntryConstructor = Class
                        .forName(TestResourceManager.TestResourceClassEntry.class.getName(), true, classLoader)
                        .getConstructor(Class.class, Map.class, Annotation.class, boolean.class, testResourceScopeClass);

                List<QuarkusTestProfile.TestResourceEntry> testResources = profileInstance.testResources();
                List<T> result = new ArrayList<>(testResources.size());
                for (QuarkusTestProfile.TestResourceEntry testResource : testResources) {
                    T instance = (T) testResourceClassEntryConstructor.newInstance(
                            Class.forName(testResource.getClazz()
                                    .getName(), true, classLoader),
                            testResource.getArgs(),
                            null, testResource.isParallel(),
                            Enum.valueOf(testResourceScopeClass, TestResourceScope.MATCHING_RESOURCES.name()));
                    result.add(instance);
                }

                return result;
            } catch (Exception e) {
                throw new IllegalStateException("Unable to handle profile " + profileInstance.getClass(), e);
            }
        }

        /**
         * Corresponds to {@link TestResourceManager#TestResourceManager(Class, Class, List, boolean, Map, Optional, Path)}
         */
        public static Closeable createReflectively(Class<?> testResourceManagerClass,
                Class<?> testClass,
                Class<?> profileClass,
                List<TestResourceManager.TestResourceClassEntry> additionalTestResources,
                boolean disableGlobalTestResources,
                Map<String, String> devServicesProperties,
                Optional<String> containerNetworkId,
                Path testClassLocation) {
            // TODO put in a bypass since sometimes we're in the canary loader
            try {
                return (Closeable) testResourceManagerClass
                        .getConstructor(Class.class, Class.class, List.class, boolean.class, Map.class, Optional.class,
                                Path.class)
                        .newInstance(testClass, profileClass, additionalTestResources, disableGlobalTestResources,
                                devServicesProperties, containerNetworkId, testClassLocation);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Corresponds to {@link TestResourceManager#TestResourceManager(Class, Class, List, boolean, Map, Optional)}
         */
        public static Closeable createReflectively(Class<?> testResourceManagerClass,
                Class<?> testClass,
                Class<?> profileClass,
                List<TestResourceManager.TestResourceClassEntry> additionalTestResources,
                boolean disableGlobalTestResources,
                Map<String, String> devServicesProperties,
                Optional<String> containerNetworkId) {
            try {
                return (Closeable) testResourceManagerClass
                        .getConstructor(Class.class, Class.class, List.class, boolean.class, Map.class, Optional.class)
                        .newInstance(testClass, profileClass, additionalTestResources, disableGlobalTestResources,
                                devServicesProperties, containerNetworkId);
            } catch (InstantiationException | IllegalArgumentException | IllegalAccessException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Corresponds to {@link TestResourceManager#init(String)}
         */
        public static void initReflectively(Object testResourceManager, Class<?> profileClassName) {
            try {
                testResourceManager.getClass()
                        .getMethod("init", String.class)
                        .invoke(testResourceManager,
                                profileClassName != null ? profileClassName.getName() : null);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                    | SecurityException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Corresponds to {@link TestResourceManager#start()}
         */
        public static Map<String, String> startReflectively(Object testResourceManager) {
            try {
                return (Map<String, String>) testResourceManager.getClass()
                        .getMethod("start")
                        .invoke(testResourceManager);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                    | SecurityException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Corresponds to {@link TestResourceManager#testResourceComparisonInfo()}
         */
        static Set<TestResourceManager.TestResourceComparisonInfo> testResourceComparisonInfo(Object testResourceManager) {
            try {
                Set originalSet = (Set) testResourceManager.getClass()
                        .getMethod("testResourceComparisonInfo")
                        .invoke(testResourceManager);
                if (originalSet.isEmpty()) {
                    return Collections.emptySet();
                }

                Set<TestResourceManager.TestResourceComparisonInfo> result = new HashSet<>(originalSet.size());
                for (var entry : originalSet) {
                    String testResourceLifecycleManagerClass = (String) entry.getClass()
                            .getMethod("testResourceLifecycleManagerClass")
                            .invoke(entry);
                    Object originalTestResourceScope = entry.getClass()
                            .getMethod("scope")
                            .invoke(entry);
                    TestResourceScope testResourceScope = null;
                    if (originalTestResourceScope != null) {
                        testResourceScope = TestResourceScope.valueOf(originalTestResourceScope.toString());
                    }
                    Object originalArgs = entry.getClass().getMethod("args").invoke(entry);
                    Map<String, Object> args = (Map<String, Object>) originalArgs;
                    result.add(new TestResourceManager.TestResourceComparisonInfo(testResourceLifecycleManagerClass,
                            testResourceScope, args));
                }

                return result;
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                    | SecurityException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
