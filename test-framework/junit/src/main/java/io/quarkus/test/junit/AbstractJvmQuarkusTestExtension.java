package io.quarkus.test.junit;

import static io.quarkus.runtime.configuration.ConfigUtils.configBuilder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.deployment.dev.testing.TestConfigCustomizer;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.Config;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigProviderResolver;

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

    /**
     * AbstractJvmQuarkusTestExtension may be loaded by different ClassLoaders.
     * <p>
     * To access Config, we need to access ConfigProviderResolver, which is registered very early in the test launch
     * process in ConfigLauncherSession. At this stage, the Config is registered in the SystemClassLoader. The
     * ClassLoader of AbstractJvmQuarkusTestExtension may not support parent delegation, which prevents
     * ConfigProviderResolver from being initialized. Therefore, we initialize it here if the ClassLoader is not the
     * SystemClassLoader, and associate it with the SystemClassLoader. Lookups to the test Config, must always use
     * the SystemClassLoader.
     */
    protected AbstractJvmQuarkusTestExtension() {
        ClassLoader classLoader = AbstractJvmQuarkusTestExtension.class.getClassLoader();
        if (classLoader != ClassLoader.getSystemClassLoader()) {
            LaunchMode current = LaunchMode.current();
            LaunchMode.set(LaunchMode.TEST);
            SmallRyeConfig config = configBuilder()
                    .forClassLoader(classLoader)
                    .withCustomizers(new TestConfigCustomizer(LaunchMode.TEST))
                    .build();
            LaunchMode.set(current);

            // Multiple Test may use the same ClassLoader, and each will instantiate a new AbstractJvmQuarkusTestExtension.
            // In these cases, the Config is already registered, so it is just easier to release it and set it again
            SmallRyeConfigProviderResolver resolver = (SmallRyeConfigProviderResolver) ConfigProviderResolver.instance();
            resolver.releaseConfig(ClassLoader.getSystemClassLoader());
            resolver.registerConfig(config, ClassLoader.getSystemClassLoader());
        }
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

        TestConfig testConfig = Config.get(ClassLoader.getSystemClassLoader()).getConfigMapping(TestConfig.class);
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
