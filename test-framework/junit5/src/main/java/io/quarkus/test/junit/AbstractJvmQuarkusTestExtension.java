package io.quarkus.test.junit;

import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Alternative;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.RestorableSystemProperties;
import io.quarkus.test.common.WithTestResource;

public class AbstractJvmQuarkusTestExtension extends AbstractQuarkusTestWithContextExtension {

    protected static final String TEST_LOCATION = "test-location";
    protected static final String TEST_CLASS = "test-class";
    protected static final String TEST_PROFILE = "test-profile";

    protected ClassLoader originalCl;

    // Used to preserve state from the previous run, so we know if we should restart an application
    protected static RunningQuarkusApplication runningQuarkusApplication;

    protected static Class<? extends QuarkusTestProfile> quarkusTestProfile;

    //needed for @Nested
    protected static final Deque<Class<?>> currentTestClassStack = new ArrayDeque<>();
    protected static Class<?> currentJUnitTestClass;

    // TODO only used by QuarkusMainTest, fix that class and delete this
    protected PrepareResult createAugmentor(ExtensionContext context, Class<? extends QuarkusTestProfile> profile,
            Collection<Runnable> shutdownTasks) throws Exception {

        originalCl = Thread.currentThread().getContextClassLoader();
        final Class<?> requiredTestClass = context.getRequiredTestClass();

        System.out.println(
                "HOLLY about to cast " + requiredTestClass.getName() + " which has cl " + requiredTestClass.getClassLoader());
        CuratedApplication curatedApplication = ((QuarkusClassLoader) requiredTestClass.getClassLoader())
                .getCuratedApplication();
        System.out.println("HOLLY " + requiredTestClass.getClassLoader() + " gives " + curatedApplication);

        // TODO need to handle the gradle case - can we put it in that method?
        Path testClassLocation = getTestClassesLocation(requiredTestClass);

        // TODO is this needed?
        //        Index testClassesIndex = TestClassIndexer.indexTestClasses(testClassLocation);
        //        // we need to write the Index to make it reusable from other parts of the testing infrastructure that run in different ClassLoaders
        //        TestClassIndexer.writeIndex(testClassesIndex, testClassLocation, requiredTestClass);

        Timing.staticInitStarted(curatedApplication.getOrCreateBaseRuntimeClassLoader(),
                curatedApplication.getQuarkusBootstrap()
                        .isAuxiliaryApplication());

        final Map<String, Object> props = new HashMap<>();
        props.put(TEST_LOCATION, testClassLocation);
        props.put(TEST_CLASS, requiredTestClass);

        // clear the test.url system property as the value leaks into the run when using different profiles
        System.clearProperty("test.url");
        Map<String, String> additional = new HashMap<>();

        QuarkusTestProfile profileInstance = getQuarkusTestProfile(profile, shutdownTasks, additional);

        if (profile != null) {
            props.put(TEST_PROFILE, profile.getName());
        }
        quarkusTestProfile = profile;
        return new PrepareResult(curatedApplication
                .createAugmentor(QuarkusTestExtension.TestBuildChainFunction.class.getName(), props), profileInstance,
                curatedApplication, testClassLocation);
    }

    protected static QuarkusTestProfile getQuarkusTestProfile(Class<? extends QuarkusTestProfile> profile,
            Collection<Runnable> shutdownTasks, Map<String, String> additional)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        QuarkusTestProfile profileInstance = null;
        if (profile != null) {
            profileInstance = profile.getConstructor().newInstance();
            additional.putAll(profileInstance.getConfigOverrides());
            if (!profileInstance.getEnabledAlternatives().isEmpty()) {
                additional.put("quarkus.arc.selected-alternatives", profileInstance.getEnabledAlternatives().stream()
                        .peek((c) -> {
                            if (!c.isAnnotationPresent(Alternative.class)) {
                                throw new RuntimeException(
                                        "Enabled alternative " + c + " is not annotated with @Alternative");
                            }
                        })
                        .map(Class::getName).collect(Collectors.joining(",")));
            }
            if (profileInstance.disableApplicationLifecycleObservers()) {
                additional.put("quarkus.arc.test.disable-application-lifecycle-observers", "true");
            }
            if (profileInstance.getConfigProfile() != null) {
                additional.put(LaunchMode.TEST.getProfileKey(), profileInstance.getConfigProfile());
            }
            //we just use system properties for now
            //it's a lot simpler
            shutdownTasks.add(RestorableSystemProperties.setProperties(additional)::close);
        }
        return profileInstance;
    }

    private ApplicationModel getGradleAppModelForIDE(Path projectRoot) throws IOException, AppModelResolverException {
        return System.getProperty(BootstrapConstants.SERIALIZED_TEST_APP_MODEL) == null
                ? BuildToolHelper.enableGradleAppModelForTest(projectRoot)
                : null;
    }

    public static Class<? extends QuarkusTestProfile> getQuarkusTestProfile(Class testClass) {
        // If the current class or any enclosing class in its hierarchy is annotated with `@TestProfile`.
        Class<? extends QuarkusTestProfile> testProfile = findTestProfileAnnotation(testClass);
        if (testProfile != null) {
            return testProfile;
        }

        // Otherwise, if the current class is annotated with `@Nested`:
        while (testClass.isAnnotationPresent(Nested.class)) {
            // let's try to find the `@TestProfile` from the enclosing classes:
            testProfile = getQuarkusTestProfile(testClass.getEnclosingClass());
            if (testProfile != null) {
                return testProfile;
            }
        }
        return null;
    }

    protected Class<? extends QuarkusTestProfile> getQuarkusTestProfile(ExtensionContext extensionContext) {
        Class testClass = extensionContext.getRequiredTestClass();
        Class testProfile = getQuarkusTestProfile(testClass);

        if (testProfile == null && (testClass.isAnnotationPresent(Nested.class))) {

            // This is unlikely to work since we recursed up the test class stack, but err on the side of double-checking?
            // if not found, let's try the parents
            Optional<ExtensionContext> parentContext = extensionContext.getParent();
            while (parentContext.isPresent()) {
                ExtensionContext currentExtensionContext = parentContext.get();
                if (currentExtensionContext.getTestClass().isEmpty()) {
                    break;
                }

                testProfile = findTestProfileAnnotation(currentExtensionContext.getTestClass().get());
                if (testProfile != null) {
                    return testProfile;
                }

                parentContext = currentExtensionContext.getParent();
            }
        }

        return null;
    }

    private static Class<? extends QuarkusTestProfile> findTestProfileAnnotation(Class<?> clazz) {
        Class<?> testClass = clazz;
        while (testClass != null) {
            TestProfile annotation = testClass.getAnnotation(TestProfile.class);
            if (annotation != null) {
                return annotation.value();
            }

            testClass = testClass.getEnclosingClass();
        }

        return null;
    }

    protected static boolean hasPerTestResources(ExtensionContext extensionContext) {
        return hasPerTestResources(extensionContext.getRequiredTestClass());
    }

    public static boolean hasPerTestResources(Class<?> requiredTestClass) {
        while (requiredTestClass != Object.class) {
            for (WithTestResource testResource : requiredTestClass.getAnnotationsByType(WithTestResource.class)) {
                if (testResource.restrictToAnnotatedClass()) {
                    return true;
                }
            }

            for (QuarkusTestResource testResource : requiredTestClass.getAnnotationsByType(QuarkusTestResource.class)) {
                if (testResource.restrictToAnnotatedClass()) {
                    return true;
                }
            }
            // scan for meta-annotations
            for (Annotation annotation : requiredTestClass.getAnnotations()) {
                // skip TestResource annotations
                var annotationType = annotation.annotationType();

                if ((annotationType != WithTestResource.class) && (annotationType != QuarkusTestResource.class)) {
                    // look for a TestResource on the annotation itself
                    if ((annotationType.getAnnotationsByType(WithTestResource.class).length > 0)
                            || (annotationType.getAnnotationsByType(QuarkusTestResource.class).length > 0)) {
                        // meta-annotations are per-test scoped for now
                        return true;
                    }
                }
            }
            // look up
            requiredTestClass = requiredTestClass.getSuperclass();
        }
        return false;
    }

    protected boolean isNewApplication(QuarkusTestExtensionState state, Class<?> currentJUnitTestClass) {

        // How do we know how to stop the current application - compare the classloader and see if it changed
        // We could also look at the running application attached to the junit test and see if it's started

        // TODO
        System.out.println(
                "HOLLY checking is new for " + currentJUnitTestClass + " with running app " + runningQuarkusApplication);
        System.out.println("HOLLY abstract cl for is new check " + this.getClass().getClassLoader());
        if (runningQuarkusApplication != null) {
            System.out.println(
                    "HOLLY checking is new " + runningQuarkusApplication.getClassLoader()
                            + currentJUnitTestClass.getClassLoader());
        }
        return (runningQuarkusApplication == null
                || runningQuarkusApplication.getClassLoader() != currentJUnitTestClass.getClassLoader());

    }

    protected static class PrepareResult {
        protected final AugmentAction augmentAction;
        protected final QuarkusTestProfile profileInstance;
        protected final CuratedApplication curatedApplication;
        protected final Path testClassLocation;

        public PrepareResult(AugmentAction augmentAction, QuarkusTestProfile profileInstance,
                CuratedApplication curatedApplication, Path testClassLocation) {
            this.augmentAction = augmentAction;
            this.profileInstance = profileInstance;
            this.curatedApplication = curatedApplication;
            this.testClassLocation = testClassLocation;
        }
    }
}
