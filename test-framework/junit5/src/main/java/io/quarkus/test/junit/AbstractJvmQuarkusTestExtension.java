package io.quarkus.test.junit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.classloading.FacadeClassLoader;
import io.smallrye.config.SmallRyeConfig;

public class AbstractJvmQuarkusTestExtension extends AbstractQuarkusTestWithContextExtension
        implements ExecutionCondition {

    protected static final String TEST_LOCATION = "test-location";
    protected static final String TEST_CLASS = "test-class";
    protected static final String TEST_PROFILE = "test-profile";

    // Used to preserve state from the previous run, so we know if we should restart an application
    protected static RunningQuarkusApplication runningQuarkusApplication;

    protected static Class<? extends QuarkusTestProfile> quarkusTestProfile;

    //needed for @Nested
    protected static final Deque<Class<?>> currentTestClassStack = new ArrayDeque<>();
    protected static Class<?> currentJUnitTestClass;

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
        boolean isFlatClasspath = this.getClass().getClassLoader() == ClassLoader.getSystemClassLoader();

        ClassLoader original = Thread.currentThread().getContextClassLoader();

        // In native mode tests, a testconfig will not have been registered on the system classloader with a testconfig instance of our classloader, so in those cases, we do not want to set the TCCL
        if (!isFlatClasspath && !(original instanceof FacadeClassLoader)) {
            // In most cases, we reset the TCCL to the system classloader after discovery finishes, so we could get away without this setting of the TCCL
            // However, in multi-module and continuous tests the TCCL lifecycle is more complex, so this setting is still needed (for now)
            Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        }

        TestConfig testConfig;
        try {
            testConfig = ConfigProvider.getConfig()
                    .unwrap(SmallRyeConfig.class)
                    .getConfigMapping(TestConfig.class);
        } catch (Exception | ServiceConfigurationError e) {
            boolean isEclipse = System.getProperty("sun.java.command") != null
                    && System.getProperty("sun.java.command").contains("JUnit5TestLoader");
            if (isEclipse) {
                // Tracked by https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2257
                Log.error(
                        "Could not read configuration while evaluating whether to run a test. This is a known issue when running tests in the Eclipse IDE. To work around the problem, edit the run configuration and add `-uniqueId [engine:junit-jupiter]/[class:"
                                + context.getRequiredTestClass().getName()
                                + "]` in the program arguments. Running the whole package, or running individual test methods, will also work without any extra configuration.");
            } else {
                Log.error("Internal error: Could not read configuration while evaluating whether to run "
                        + context.getRequiredTestClass()
                        + ". Please let the Quarkus team know what you were doing when this error happened.");

            }
            Log.debug("Underlying exception: " + e);
            Log.debug("Thread Context Classloader: " + Thread.currentThread().getContextClassLoader());
            Log.debug("The class of the class we use for mapping is " + TestConfig.class.getClassLoader());
            throw new IllegalStateException("Non-viable test classloader, " + Thread.currentThread().getContextClassLoader()
                    + ". Is this a re-run of a failing test?");
        } finally {
            if (!isFlatClasspath) {
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
}
