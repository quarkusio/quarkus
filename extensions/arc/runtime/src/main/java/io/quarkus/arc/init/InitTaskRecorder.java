package io.quarkus.arc.init;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import org.eclipse.microprofile.config.ConfigProvider;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.PreventFurtherStepsException;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Initialization;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.init.InitRuntimeConfig;
import io.quarkus.runtime.util.PatternUtil;

/**
 * A {@link Recorder} that deals with initialization tasks.
 * It's responsible for executing user provided {@link Initialization} tasks.
 * It's also used to check if the application should exit once all initialization tasks are completed.
 */
@Recorder
public class InitTaskRecorder {

    private static final String QUARKUS_INIT_AND_EXIT = "quarkus.init-and-exit";
    private final RuntimeValue<InitRuntimeConfig> config;

    public InitTaskRecorder(RuntimeValue<InitRuntimeConfig> config) {
        this.config = config;
    }

    public void executeInitializationTask(String taskName) {
        if (config.getValue().initDisabled) {
            return;
        }

        if (!PatternUtil.matches(taskName, config.getValue().initTaskFilter)) {
            return;
        }

        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(Runnable.class, Initialization.Literal.forName(taskName));
        for (Bean<?> bean : beans) {
            Bean<Runnable> runnableBean = (Bean<Runnable>) bean;
            CreationalContext<Runnable> ctx = beanManager.createCreationalContext(runnableBean);
            Runnable runnable = (Runnable) beanManager.getReference(runnableBean, Runnable.class, ctx);
            runnable.run();
        }
    }

    public void exitIfNeeded() {
        //The tcks CustomConverTest is broken: It always returns true for boolean values.
        //To workaround this issue, we need to check if `init-and-exit` is explicitly defined.
        boolean initAndExitConfigured = StreamSupport.stream(ConfigProvider.getConfig().getPropertyNames().spliterator(), false)
                .anyMatch(n -> QUARKUS_INIT_AND_EXIT.equals(n));
        if (initAndExitConfigured) {
            if (config.getValue().initAndExit) {
                preventFurtherRecorderSteps(5, "Error attempting to gracefully shutdown after initialization",
                        () -> new PreventFurtherStepsException("Gracefully exiting after initialization.", 0));
            }
        }
    }

    public static void preventFurtherRecorderSteps(int waitSeconds, String waitErrorMessage,
            Supplier<PreventFurtherStepsException> supplier) {
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Quarkus.blockingExit();
                latch.countDown();
            }
        }).start();
        try {
            latch.await(waitSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println(waitErrorMessage);
        }
        throw supplier.get();
    }
}
