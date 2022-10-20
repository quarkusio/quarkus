package io.quarkus.test.junit;

import java.io.IOException;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;

public abstract class AbstractQuarkusTestWithContextExtension extends AbstractTestWithCallbacksExtension
        implements LifecycleMethodExecutionExceptionHandler, TestWatcher {
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
        return getStoreFromContext(context).get(QuarkusTestExtensionState.class.getName(), QuarkusTestExtensionState.class);
    }

    protected void setState(ExtensionContext context, QuarkusTestExtensionState state) {
        getStoreFromContext(context).put(QuarkusTestExtensionState.class.getName(), state);
    }

    protected void clearState(ExtensionContext context) {
        QuarkusTestExtensionState state = getState(context);
        if (state != null) {
            try {
                state.close();
            } catch (IOException ignored) {
                // ignore errors when clearing out the context.
            } finally {
                getStoreFromContext(context).remove(QuarkusTestExtensionState.class.getName());
            }
        }
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
