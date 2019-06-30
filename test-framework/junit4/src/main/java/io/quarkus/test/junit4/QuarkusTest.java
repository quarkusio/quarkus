package io.quarkus.test.junit4;

import static io.quarkus.test.common.PathTestHelper.getAppClassLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.IOException;
import java.util.function.Consumer;

import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import io.quarkus.runner.RuntimeRunner;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.common.PropertyTestUtil;
import io.quarkus.test.common.TestInjectionManager;
import io.quarkus.test.common.TestInstantiator;
import io.quarkus.test.common.http.TestHTTPResourceManager;

public class QuarkusTest extends AbstractQuarkusTestRunner {

    public QuarkusTest(Class<?> klass) throws InitializationError {
        super(klass, (c, n) -> new QuarkusRunListener(c, n));
    }

    private static class QuarkusRunListener extends AbstractQuarkusRunListener {

        private RuntimeRunner runtimeRunner;

        QuarkusRunListener(Class<?> testClass, RunNotifier runNotifier) {
            super(testClass, runNotifier);
        }

        @Override
        protected void startQuarkus() {
            PropertyTestUtil.setLogFileProperty();
            runtimeRunner = RuntimeRunner.builder()
                    .setLaunchMode(LaunchMode.TEST)
                    .setClassLoader(getClass().getClassLoader())
                    .setTarget(getAppClassLocation(getTestClass()))
                    .setFrameworkClassesPath(getTestClassesLocation(getTestClass()))
                    .addChainCustomizer(new Consumer<BuildChainBuilder>() {
                        @Override
                        public void accept(BuildChainBuilder buildChainBuilder) {
                            buildChainBuilder.addBuildStep(new BuildStep() {
                                @Override
                                public void execute(BuildContext context) {
                                    context.produce(new TestAnnotationBuildItem(RunWith.class.getName()));
                                }
                            }).produces(TestAnnotationBuildItem.class)
                                    .build();
                        }
                    })
                    .build();
            runtimeRunner.run();
        }

        @Override
        protected void stopQuarkus() throws IOException {
            runtimeRunner.close();
        }
    }

    @Override
    protected Object createTest() throws Exception {
        Object instance = TestInstantiator.instantiateTest(getTestClass().getJavaClass());
        TestHTTPResourceManager.inject(instance);
        TestInjectionManager.inject(instance);
        quarkusRunListener.inject(instance);
        return instance;
    }
}
