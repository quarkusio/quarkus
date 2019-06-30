package io.quarkus.test.junit4;

import java.util.function.BiFunction;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import io.quarkus.test.common.http.TestHTTPResourceManager;

abstract class AbstractQuarkusTestRunner extends BlockJUnit4ClassRunner {

    private static boolean first = true;
    protected static AbstractQuarkusRunListener quarkusRunListener;

    private final BiFunction<Class<?>, RunNotifier, AbstractQuarkusRunListener> quarkusRunListenerSupplier;

    public AbstractQuarkusTestRunner(Class<?> klass,
            BiFunction<Class<?>, RunNotifier, AbstractQuarkusRunListener> quarkusRunListenerSupplier)
            throws InitializationError {
        super(klass);
        this.quarkusRunListenerSupplier = quarkusRunListenerSupplier;
    }

    @Override
    public void run(final RunNotifier notifier) {
        if (first) {
            first = false;
            quarkusRunListener = quarkusRunListenerSupplier.apply(getTestClass().getJavaClass(), notifier);
            notifier.addListener(quarkusRunListener);
        }

        super.run(notifier);
    }

    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        if (!quarkusRunListener.isFailed()) {
            super.runChild(method, notifier);
        } else {
            notifier.fireTestIgnored(describeChild(method));
        }
    }

    @Override
    protected Object createTest() throws Exception {
        Object instance = super.createTest();
        TestHTTPResourceManager.inject(instance);
        return instance;
    }
}
