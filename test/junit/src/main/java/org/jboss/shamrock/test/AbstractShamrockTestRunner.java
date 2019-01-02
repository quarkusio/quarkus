package org.jboss.shamrock.test;

import java.util.function.BiFunction;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

abstract class AbstractShamrockTestRunner extends BlockJUnit4ClassRunner {

    private static boolean first = true;
    private static AbstractShamrockRunListener shamrockRunListener;

    private final BiFunction<Class<?>, RunNotifier, AbstractShamrockRunListener> shamrockRunListenerSupplier;

    public AbstractShamrockTestRunner(Class<?> klass, BiFunction<Class<?>, RunNotifier, AbstractShamrockRunListener> shamrockRunListenerSupplier) throws InitializationError {
        super(klass);
        this.shamrockRunListenerSupplier = shamrockRunListenerSupplier;
    }

    @Override
    public void run(final RunNotifier notifier) {
        if (first) {
            first = false;
            shamrockRunListener = shamrockRunListenerSupplier.apply(getTestClass().getJavaClass(), notifier);
            notifier.addListener(shamrockRunListener);
        }

        super.run(notifier);
    }

    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        if (!shamrockRunListener.isFailed()) {
            super.runChild(method, notifier);
        } else {
            notifier.fireTestIgnored(describeChild(method));
        }
    }
}
