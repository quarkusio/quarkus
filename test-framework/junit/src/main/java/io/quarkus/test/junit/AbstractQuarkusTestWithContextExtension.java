package io.quarkus.test.junit;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import java.io.IOException;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;

import io.quarkus.test.config.ConfigInjector;
import io.quarkus.test.config.ValueRegistryInjector;
import io.quarkus.value.registry.ValueRegistry;
import io.smallrye.config.Config;
import io.smallrye.config.SmallRyeConfigProviderResolver;

public abstract class AbstractQuarkusTestWithContextExtension extends AbstractTestWithCallbacksExtension
        implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler, TestWatcher {

    private static final Logger LOG = Logger.getLogger(AbstractQuarkusTestWithContextExtension.class);

    public static final String IO_QUARKUS_TESTING_TYPE = "io.quarkus.testing.type";

    private static final String MAVEN_FORK_STATE = "io.quarkus.test.junit.internal.MavenForkState";

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        markTestAsFailed(context, throwable);

        throw throwable;
    }

    @Override
    public void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        markTestAsFailed(context, throwable);

        throw throwable;
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        markTestAsFailed(context, throwable);

        throw throwable;
    }

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        markTestAsFailed(context, throwable);

        throw throwable;
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        markTestAsFailed(context, throwable);

        throw throwable;
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        markTestAsFailed(context, cause);
    }

    protected QuarkusTestExtensionState getState(ExtensionContext context) {
        ExtensionContext.Store store = getStoreFromContext(context);
        boolean mavenFork = isMavenFork();
        Object[] mavenForkState = mavenFork ? getMavenForkState() : null;
        Object o = mavenFork ? mavenForkState[0] : store.get(QuarkusTestExtensionState.class.getName());
        if (o != null) {

            QuarkusTestExtensionState state;

            // It's quite possible the state was created in another classloader, and if so, we will need to clone it across into this classloader
            if (o instanceof QuarkusTestExtensionState) {
                state = (QuarkusTestExtensionState) o;
            } else {
                state = QuarkusTestExtensionState.clone(o);
            }

            Object testingTypeOfState = mavenFork ? mavenForkState[1]
                    : store.get(IO_QUARKUS_TESTING_TYPE, Class.class);
            boolean sameTestingType = mavenFork ? this.getTestingType().getName().equals(testingTypeOfState)
                    : this.getTestingType().equals(testingTypeOfState);
            if (!sameTestingType) {
                // The current state was created by a different testing type, so we need to renew it, so the new state is
                // compatible with the current testing type.
                try {
                    state.close();
                } catch (IOException ignored) {
                    LOG.debug(ignored);
                    // ignoring exceptions when closing state.

                } finally {
                    if (mavenFork) {
                        setMavenForkState(null, null, null, null, null);
                    } else {
                        store.remove(QuarkusTestExtensionState.class.getName());
                    }
                    store.remove(ValueRegistry.class.getName());
                    store.remove(Config.class.getName());
                    ValueRegistryInjector.clear(context);
                    ConfigInjector.clear(context);
                }

                return null;
            }
            if (mavenFork) {
                restoreMavenForkContext(context, store, mavenForkState);
            }
            return state;

        } else {
            return null;
        }

    }

    protected void setState(ExtensionContext context, QuarkusTestExtensionState state) {
        ExtensionContext.Store store = getStoreFromContext(context);
        Object valueRegistry = context.getStore(GLOBAL).get(ValueRegistry.class.getName());
        Object config = context.getStore(GLOBAL).get(Config.class.getName());
        store.put(ValueRegistry.class.getName(), valueRegistry);
        store.put(Config.class.getName(), config);
        if (isMavenFork()) {
            setMavenForkState(state, state == null ? null : this.getTestingType().getName(),
                    state == null ? null : valueRegistry,
                    state == null ? null : config,
                    state == null ? null : context.getRoot());
        } else {
            store.put(QuarkusTestExtensionState.class.getName(), state);
            store.put(IO_QUARKUS_TESTING_TYPE, this.getTestingType());
        }
    }

    protected ExtensionContext.Store getStoreFromContext(ExtensionContext context) {
        // TODO if we would add some ugly code here to jump up to the
        // system classloader, we could load QuarkusTestExtension with the test's classloader, and
        // avoid a whole bunch of reflection
        // TODO #store

        ExtensionContext root = context.getRoot();
        return root.getStore(GLOBAL);
    }

    protected void markTestAsFailed(ExtensionContext context, Throwable throwable) {
        QuarkusTestExtensionState state = getState(context);
        if (state != null) {
            state.setTestFailed(throwable);
        }
    }

    protected static boolean isMavenFork() {
        return System.getProperty("surefire.real.class.path") != null;
    }

    private static Object[] getMavenForkState() {
        try {
            return (Object[]) getMavenForkStateStore().getMethod("get").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to read Quarkus test state for the Maven fork", e);
        }
    }

    private static void restoreMavenForkContext(ExtensionContext context, ExtensionContext.Store store,
            Object[] mavenForkState) {
        Object valueRegistry = mavenForkState[2];
        if (mavenForkState[4] == context.getRoot()) {
            return;
        }
        Object config = mavenForkState[3];
        if (valueRegistry != null) {
            store.put(ValueRegistry.class.getName(), valueRegistry);
            context.getStore(GLOBAL).put(ValueRegistry.class.getName(), valueRegistry);
        }
        if (config != null) {
            store.put(Config.class.getName(), config);
            context.getStore(GLOBAL).put(Config.class.getName(), config);
        }
        setMavenForkState(mavenForkState[0], (String) mavenForkState[1], valueRegistry, config, context.getRoot());
    }

    protected static void restoreMavenForkConfig(ExtensionContext context) {
        Config config = context.getStore(GLOBAL).get(Config.class.getName(), Config.class);
        SmallRyeConfigProviderResolver resolver = (SmallRyeConfigProviderResolver) ConfigProviderResolver.instance();
        ClassLoader testClassLoader = context.getRequiredTestClass().getClassLoader();
        registerConfig(resolver, config, testClassLoader);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != testClassLoader) {
            registerConfig(resolver, config, contextClassLoader);
        }
    }

    private static void registerConfig(SmallRyeConfigProviderResolver resolver, Config config, ClassLoader classLoader) {
        resolver.releaseConfig(classLoader);
        resolver.registerConfig(config, classLoader);
    }

    private static void setMavenForkState(Object state, String testingType, Object valueRegistry, Object config,
            Object contextRoot) {
        try {
            getMavenForkStateStore()
                    .getMethod("set", Object.class, String.class, Object.class, Object.class, Object.class)
                    .invoke(null, state, testingType, valueRegistry, config, contextRoot);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to store Quarkus test state for the Maven fork", e);
        }
    }

    private static Class<?> getMavenForkStateStore() {
        try {
            // Quarkus test classloaders can change when profiles do, while the system classloader spans the Maven fork.
            return ClassLoader.getSystemClassLoader().loadClass(MAVEN_FORK_STATE);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load the Quarkus test state store for the Maven fork", e);
        }
    }
}
