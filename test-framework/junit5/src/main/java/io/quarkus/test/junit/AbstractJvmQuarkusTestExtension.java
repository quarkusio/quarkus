package io.quarkus.test.junit;

import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Alternative;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.logging.Log;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.common.RestorableSystemProperties;
import io.smallrye.config.SmallRyeConfig;

public class AbstractJvmQuarkusTestExtension extends AbstractQuarkusTestWithContextExtension
        implements ExecutionCondition {

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

        CuratedApplication curatedApplication = getCuratedApplication(requiredTestClass, context, shutdownTasks);

        // TODO need to handle the gradle case - can we put it in that method?
        Path testClassLocation = getTestClassesLocation(requiredTestClass, curatedApplication);

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
                .createAugmentor(TestBuildChainFunction.class.getName(), props), profileInstance,
                curatedApplication, testClassLocation);
    }

    protected CuratedApplication getCuratedApplication(Class<?> requiredTestClass, ExtensionContext context,
            Collection<Runnable> shutdownTasks) throws BootstrapException, AppModelResolverException, IOException {
        // TODO make this abstract, push this implementation down to QuarkusTestExtension, since that is the only place it will work
        CuratedApplication curatedApplication = ((QuarkusClassLoader) requiredTestClass.getClassLoader())
                .getCuratedApplication();
        return curatedApplication;
    }

    protected static QuarkusTestProfile getQuarkusTestProfile(Class<? extends QuarkusTestProfile> profile,
            Collection<Runnable> shutdownTasks, Map<String, String> additional)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        QuarkusTestProfile profileInstance = null;
        if (profile != null) {
            profileInstance = profile.getConstructor().newInstance();
            // TODO the stuff below here is unique to this class - TODO what does this comment even mean?
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

    // TODO is it nicer to pass in the test class, or invoke the getter twice?
    public static Class<? extends QuarkusTestProfile> getQuarkusTestProfile(Class testClass,
            ExtensionContext extensionContext) {
        // If the current class or any enclosing class in its hierarchy is annotated with `@TestProfile`.
        Class<? extends QuarkusTestProfile> testProfile = findTestProfileAnnotation(testClass);
        if (testProfile != null) {
            return testProfile;
        }

        // Otherwise, if the current class is annotated with `@Nested`:
        if (testClass.isAnnotationPresent(Nested.class)) {
            // let's try to find the `@TestProfile` from the enclosing classes:
            testProfile = findTestProfileAnnotation(testClass.getEnclosingClass());
            if (testProfile != null) {
                return testProfile;
            }

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

    protected Class<? extends QuarkusTestProfile> getQuarkusTestProfile(ExtensionContext extensionContext) {
        Class testClass = extensionContext.getRequiredTestClass();
        Class testProfile = getQuarkusTestProfile(testClass, extensionContext);

        if (testProfile != null) {
            return testProfile;
        }

        if (testClass.isAnnotationPresent(Nested.class)) {

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

    protected boolean isNewApplication(QuarkusTestExtensionState state, Class<?> currentJUnitTestClass) {

        // How do we know how to stop the current application - compare the classloader and see if it changed
        // We could also look at the running application attached to the junit test and see if it's started

        return (runningQuarkusApplication == null
                || runningQuarkusApplication.getClassLoader() != currentJUnitTestClass.getClassLoader());

    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (!context.getTestClass().isPresent()) {
            return ConditionEvaluationResult.enabled("No test class specified");
        }
        if (context.getTestInstance().isPresent()) {
            return ConditionEvaluationResult.enabled("Quarkus Test Profile tags only affect classes");
        }

        // At this point, the TCCL is usually the FacadeClassLoader, but sometimes it's a deployment classloader (for multimodule tests), or the runtime classloader (for nested tests)
        // Getting back to the FacadeClassLoader is non-trivial. We can't use the singleton on the class, because we will be accessing it from different classloaders.
        // We can't have a hook back from the runtime classloader to the facade classloader, because
        // when evaluating execution conditions for native tests, the test will have been loaded with the system classloader, not the runtime classloader.
        // The one classloader we can reliably get to when evaluating test execution is the system classloader, so hook our config on that.

        // To avoid instanceof check, check for the system classloader instead of checking for the quarkusclassloader
        boolean isRunningOnSystem = this.getClass().getClassLoader() == ClassLoader.getSystemClassLoader();

        ClassLoader original = Thread.currentThread().getContextClassLoader();

        // In native mode tests, a testconfig will not have been registered on the system classloader with a testconfig instance of our classloader, so in those cases, we do not want to set the TCCL
        if (!isRunningOnSystem) {
            Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        }

        TestConfig testConfig;
        try {
            testConfig = ConfigProvider.getConfig()
                    .unwrap(SmallRyeConfig.class)
                    .getConfigMapping(TestConfig.class);
        } catch (Exception | ServiceConfigurationError e) {
            // Tracked by https://github.com/quarkusio/quarkus/issues/46048
            Log.error("Could not read configuration while evaluating whether to run " + context.getRequiredTestClass()
                    + ". This usually happens when re-running a test that has already failed, for example if surefire.rerunFailingTestsCount is set. To work around this limitation, either adjust the test so that it passes, or isolate the test into a project whose tests all use the same combination of @TestProfile and resources.");
            Log.debug("Underlying exception: " + e);
            Log.debug("Thread Context Classloader: " + Thread.currentThread().getContextClassLoader());
            Log.debug("The class of the class we use for mapping is " + TestConfig.class.getClassLoader());
            throw new IllegalStateException("Non-viable test classloader, " + Thread.currentThread().getContextClassLoader()
                    + ". Is this a re-run of a failing test?");
        } finally {
            if (!isRunningOnSystem) {
                Thread.currentThread().setContextClassLoader(original);
            }
        }

        Optional<List<String>> tags = testConfig.profile().tags();
        if (tags.isEmpty() || tags.get().isEmpty()) {
            return ConditionEvaluationResult.enabled("No Quarkus Test Profile tags");
        }

        Class<? extends QuarkusTestProfile> testProfile = getQuarkusTestProfile(context);
        if (testProfile == null) {
            return ConditionEvaluationResult.disabled("Test '" + context.getRequiredTestClass()
                    + "' is not annotated with '@QuarkusTestProfile' but 'quarkus.profile.test.tags' was set");
        }
        QuarkusTestProfile profileInstance;
        try {
            profileInstance = testProfile.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Set<String> testProfileTags = profileInstance.tags();
        for (String tag : tags.get()) {
            String trimmedTag = tag.trim();
            if (testProfileTags.contains(trimmedTag)) {
                return ConditionEvaluationResult.enabled("Tag '" + trimmedTag + "' is present on '" + testProfile
                        + "' which is used on test '" + context.getRequiredTestClass());
            }
        }
        return ConditionEvaluationResult.disabled("Test '" + context.getRequiredTestClass()
                + "' disabled because 'quarkus.profile.test.tags' don't match the tags of '" + testProfile + "'");
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
