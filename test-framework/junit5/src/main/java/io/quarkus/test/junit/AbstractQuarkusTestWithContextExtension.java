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
        QuarkusTestExtensionState state = store.get(QuarkusTestExtensionState.class.getName(), QuarkusTestExtensionState.class);
        if (state != null) {
            Class<?> testingTypeOfState = store.get(IO_QUARKUS_TESTING_TYPE, Class.class);
            if (!this.getTestingType().equals(testingTypeOfState)) {
                // The current state was created by a different testing type, so we need to renew it, so the new state is
                // compatible with the current testing type.
                try {
                    state.close();
                } catch (IOException ignored) {
                    // ignoring exceptions when closing state.
                } finally {
                    getStoreFromContext(context).remove(QuarkusTestExtensionState.class.getName());
                }

                return null;
            }
        }

        return state;
    }

    protected void setState(ExtensionContext context, QuarkusTestExtensionState state) {
        ExtensionContext.Store store = getStoreFromContext(context);
        store.put(QuarkusTestExtensionState.class.getName(), state);
        store.put(IO_QUARKUS_TESTING_TYPE, this.getTestingType());
    }

    protected ExtensionContext.Store getStoreFromContext(ExtensionContext context) {
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
