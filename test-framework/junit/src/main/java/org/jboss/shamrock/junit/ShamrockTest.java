package org.jboss.shamrock.junit;

import java.lang.reflect.Method;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class ShamrockTest extends BlockJUnit4ClassRunner {

    private static boolean first = true;

    /**
     * Creates a BlockJUnit4ClassRunner to run {@code klass}
     *
     * @param klass
     * @throws InitializationError if the test class is malformed.
     */
    public ShamrockTest(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    public void run(final RunNotifier notifier) {

        runInternal(notifier);
        super.run(notifier);
    }

    private void runInternal(RunNotifier notifier) {
        if (first) {
            first = false;
            //now we need to bootstrap shamrock
            try {
                Class<?> mainClass = Class.forName("org.jboss.shamrock.runner.Main");
                Method main = mainClass.getDeclaredMethod("main", String[].class);
                main.invoke(null, (Object) new String[0]);
                notifier.addListener(new RunListener(){
                    @Override
                    public void testRunFinished(Result result) throws Exception {
                        super.testRunFinished(result);
                        //TODO: how do we stop shamrock?
                    }
                });
            } catch (Exception e) {
                notifier.fireTestFailure(new Failure(Description.createSuiteDescription(ShamrockTest.class), e));
            }

        }
    }
}
