package io.quarkus.test.junit4;

import java.io.IOException;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import io.quarkus.test.common.NativeImageLauncher;

/**
 * A test runner for GraalVM native images.
 */
public class SubstrateTest extends AbstractQuarkusTestRunner {

    public SubstrateTest(Class<?> klass) throws InitializationError {
        super(klass, (c, n) -> new QuarkusNativeImageRunListener(c, n));
    }

    private static class QuarkusNativeImageRunListener extends AbstractQuarkusRunListener {

        private NativeImageLauncher quarkusProcess;

        QuarkusNativeImageRunListener(Class<?> testClass, RunNotifier runNotifier) {
            super(testClass, runNotifier);
        }

        @Override
        protected void startQuarkus() throws IOException {
            quarkusProcess = new NativeImageLauncher(getTestClass());
            try {
                quarkusProcess.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void stopQuarkus() {
            quarkusProcess.close();
        }
    }
}
