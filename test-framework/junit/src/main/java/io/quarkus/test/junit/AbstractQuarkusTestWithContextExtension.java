package io.quarkus.test.junit;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import java.io.IOException;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;

import io.quarkus.value.registry.ValueRegistry;
import io.smallrye.config.Config;

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
                        setMavenForkState(null, null);
                    } else {
                        store.remove(QuarkusTestExtensionState.class.getName());
                    }
                }

                return null;
            }
            return state;

        } else {
            return null;
        }

    }

    protected void setState(ExtensionContext context, QuarkusTestExtensionState state) {
        ExtensionContext.Store store = getStoreFromContext(context);
        store.put(ValueRegistry.class.getName(), context.getStore(GLOBAL).get(ValueRegistry.class.getName()));
        store.put(Config.class.getName(), context.getStore(GLOBAL).get(Config.class.getName()));
        if (isMavenFork()) {
            setMavenForkState(state, state == null ? null : this.getTestingType().getName());
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

    private static boolean isMavenFork() {
        return System.getProperty("surefire.real.class.path") != null;
    }

    private static Object[] getMavenForkState() {
        try {
            return (Object[]) getMavenForkStateStore().getMethod("get").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to read Quarkus test state for the Maven fork", e);
        }
    }

    private static void setMavenForkState(Object state, String testingType) {
        try {
            getMavenForkStateStore().getMethod("set", Object.class, String.class).invoke(null, state, testingType);
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
