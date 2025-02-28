package io.quarkus.test.junit;

import java.io.IOException;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;

public abstract class AbstractQuarkusTestWithContextExtension extends AbstractTestWithCallbacksExtension
        implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler, TestWatcher {

    public static final String IO_QUARKUS_TESTING_TYPE = "io.quarkus.testing.type";

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
        Object o = store.get(QuarkusTestExtensionState.class.getName());
        //    QuarkusTestExtensionState state = store.get(QuarkusTestExtensionState.class.getName(), QuarkusTestExtensionState.class);
        if (o != null) {
            //            QuarkusTestExtensionState state = (QuarkusTestExtensionState) new NewSerializingDeepClone(
            //                    o.getClass().getClassLoader(),
            //                    this.getClass().getClassLoader()).clone(o);

            QuarkusTestExtensionState state;

            if (o instanceof QuarkusTestExtensionState) {
                state = (QuarkusTestExtensionState) o;
            } else {
                state = QuarkusTestExtensionState.clone(o);
            }

            Class<?> testingTypeOfState = store.get(IO_QUARKUS_TESTING_TYPE, Class.class);
            if (!this.getTestingType().equals(testingTypeOfState)) {
                // The current state was created by a different testing type, so we need to renew it, so the new state is
                // compatible with the current testing type.
                try {
                    //                    Method closeMethod = state.getClass().getMethod("close");
                    //                    closeMethod.invoke(state);
                    state.close();
                } catch (IOException ignored) {
                    // ignoring exceptions when closing state.

                } finally {
                    getStoreFromContext(context).remove(QuarkusTestExtensionState.class.getName());
                }

                return null;
            }
            return state;

        } else {
            return null;
        }

    }

    protected void setState(ExtensionContext context, QuarkusTestExtensionState state) {
        System.out.println("HOLLY setting state " + state.getClass() + " cl:" + state.getClass().getClassLoader());
        ExtensionContext.Store store = getStoreFromContext(context);
        store.put(QuarkusTestExtensionState.class.getName(), state);
        store.put(IO_QUARKUS_TESTING_TYPE, this.getTestingType());
    }

    protected ExtensionContext.Store getStoreFromContext(ExtensionContext context) {
        // TODO if we would add some ugly code here to jump up to the
        // system classloader, we could load QuarkusTestExtension with the test's classloader, and
        // avoid a whole bunch of reflection
        // TODO #store

        ExtensionContext root = context.getRoot();
        return root.getStore(ExtensionContext.Namespace.GLOBAL);
    }

    protected void markTestAsFailed(ExtensionContext context, Throwable throwable) {
        QuarkusTestExtensionState state = getState(context);
        if (state != null) {
            state.setTestFailed(throwable);
        }
    }
}
