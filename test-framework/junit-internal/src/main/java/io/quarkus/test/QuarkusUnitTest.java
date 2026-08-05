package io.quarkus.test;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.LogRecord;

import org.jboss.shrinkwrap.api.spec.JavaArchive;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.classloading.ClassLoaderEventListener;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.maven.dependency.Dependency;

/**
 * @deprecated use {@link QuarkusExtensionTest}
 */
// When we delete this, also remove the superclass and consolidate function back into QuarkusExtensionTest
@Deprecated(since = "3.35", forRemoval = true)
public class QuarkusUnitTest extends AbstractQuarkusExtensionTest<QuarkusUnitTest> {

    public QuarkusUnitTest() {
        super();
    }

    public QuarkusUnitTest(boolean useSecureConnection) {
        super(useSecureConnection);
    }

    public static QuarkusUnitTest withSecuredConnection() {
        return new QuarkusUnitTest(true);
    }

    // Bridge methods for binary compatibility.
    // The superclass methods return the generic type S which erases to AbstractQuarkusExtensionTest.
    // Code compiled against the old non-generic QuarkusUnitTest expects methods returning QuarkusUnitTest.

    @Override
    public QuarkusUnitTest setArchiveProducer(Supplier<JavaArchive> archiveProducer) {
        return super.setArchiveProducer(archiveProducer);
    }

    @Override
    public QuarkusUnitTest withApplicationRoot(Consumer<JavaArchive> applicationRootConsumer) {
        return super.withApplicationRoot(applicationRootConsumer);
    }

    @Override
    public QuarkusUnitTest withEmptyApplication() {
        return super.withEmptyApplication();
    }

    @Override
    public QuarkusUnitTest overrideConfigKey(String propertyKey, String propertyValue) {
        return super.overrideConfigKey(propertyKey, propertyValue);
    }

    @Override
    public QuarkusUnitTest overrideRuntimeConfigKey(String propertyKey, String propertyValue) {
        return super.overrideRuntimeConfigKey(propertyKey, propertyValue);
    }

    @Override
    public QuarkusUnitTest withConfigurationResource(String resourceName) {
        return super.withConfigurationResource(resourceName);
    }

    @Override
    public QuarkusUnitTest assertException(Consumer<Throwable> assertException) {
        return super.assertException(assertException);
    }

    @Override
    public QuarkusUnitTest setExpectedException(Class<? extends Throwable> expectedException) {
        return super.setExpectedException(expectedException);
    }

    @Override
    public QuarkusUnitTest setExpectedException(Class<? extends Throwable> expectedException, boolean logMessage) {
        return super.setExpectedException(expectedException, logMessage);
    }

    @Override
    public QuarkusUnitTest setForcedDependencies(List<Dependency> forcedDependencies) {
        return super.setForcedDependencies(forcedDependencies);
    }

    @Override
    public QuarkusUnitTest addBuildChainCustomizer(Consumer<BuildChainBuilder> customizer) {
        return super.addBuildChainCustomizer(customizer);
    }

    @Override
    public QuarkusUnitTest setLogRecordPredicate(Predicate<LogRecord> predicate) {
        return super.setLogRecordPredicate(predicate);
    }

    @Override
    public QuarkusUnitTest assertLogRecords(Consumer<List<LogRecord>> assertLogRecords) {
        return super.assertLogRecords(assertLogRecords);
    }

    @Override
    public QuarkusUnitTest setLogFileName(String logFileName) {
        return super.setLogFileName(logFileName);
    }

    @Override
    public QuarkusUnitTest setBeforeAllCustomizer(Runnable beforeAllCustomizer) {
        return super.setBeforeAllCustomizer(beforeAllCustomizer);
    }

    @Override
    public QuarkusUnitTest setAfterAllCustomizer(Runnable afterAllCustomizer) {
        return super.setAfterAllCustomizer(afterAllCustomizer);
    }

    @Override
    public QuarkusUnitTest setCommandLineParameters(String... commandLineParameters) {
        return super.setCommandLineParameters(commandLineParameters);
    }

    @Override
    public QuarkusUnitTest withAdditionalDependency(Consumer<JavaArchive> dependencyConsumer) {
        return super.withAdditionalDependency(dependencyConsumer);
    }

    @Override
    public QuarkusUnitTest addAdditionalDependency(JavaArchive archive) {
        return super.addAdditionalDependency(archive);
    }

    @Override
    public QuarkusUnitTest withConfiguration(String configBlock) {
        return super.withConfiguration(configBlock);
    }

    @Override
    public QuarkusUnitTest withRuntimeConfiguration(String configBlock) {
        return super.withRuntimeConfiguration(configBlock);
    }

    @Override
    public QuarkusUnitTest setFlatClassPath(boolean flatClassPath) {
        return super.setFlatClassPath(flatClassPath);
    }

    @Override
    public QuarkusUnitTest setAllowTestClassOutsideDeployment(boolean allowTestClassOutsideDeployment) {
        return super.setAllowTestClassOutsideDeployment(allowTestClassOutsideDeployment);
    }

    @Override
    public QuarkusUnitTest setAfterUndeployListener(Runnable afterUndeployListener) {
        return super.setAfterUndeployListener(afterUndeployListener);
    }

    @Override
    public QuarkusUnitTest addClassLoaderEventListener(ClassLoaderEventListener listener) {
        return super.addClassLoaderEventListener(listener);
    }

    @Override
    public QuarkusUnitTest addBootstrapCustomizer(Consumer<QuarkusBootstrap.Builder> consumer) {
        return super.addBootstrapCustomizer(consumer);
    }

    @Override
    public QuarkusUnitTest debugBytecode(boolean debugBytecode) {
        return super.debugBytecode(debugBytecode);
    }

    @Override
    public QuarkusUnitTest traceCategories(String... categories) {
        return super.traceCategories(categories);
    }
}
