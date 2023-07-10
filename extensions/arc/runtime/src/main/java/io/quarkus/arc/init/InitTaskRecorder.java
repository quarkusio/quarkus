package io.quarkus.arc.init;

import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.runtime.PreventFurtherStepsException;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.PreStart;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.init.InitRuntimeConfig;
import io.quarkus.runtime.util.PatternUtil;
import io.quarkus.runtime.util.StringUtil;

/**
 * A {@link Recorder} that deals with initialization tasks.
 * It's responsible for executing user provided {@link PreStart} tasks.
 * It's also used to check if the application should exit once all initialization tasks are completed.
 */
@Recorder
public class InitTaskRecorder {

    private static final String QUARKUS_INIT_AND_EXIT = "quarkus.init-and-exit";
    private static final String QUARKUS_INIT_DISABLED = "quarkus.init-disabled";

    private static final String UNAMED = "";
    private final RuntimeValue<InitRuntimeConfig> config;

    public InitTaskRecorder(RuntimeValue<InitRuntimeConfig> config) {
        this.config = config;
    }

    public void executeInitializationTask(String taskName) {
        boolean initDisabledConfigured = propertyConfigured(QUARKUS_INIT_DISABLED);
        if (initDisabledConfigured && config.getValue().initDisabled) {
            return;
        }

        if (!PatternUtil.matches(taskName, config.getValue().initTaskFilter)) {
            return;
        }

        InjectableInstance<Runnable> instances = Arc.container().select(Runnable.class, PreStart.Literal.forName(taskName));
        Set<Runnable> runnables = stream(instances).collect(Collectors.toSet());
        if (runnables.isEmpty()) {
            // If no instance found, then let's check task with no explicit name.
            // Such tasks by convention are named after the bearing class.
            instances = Arc.container().select(Runnable.class, PreStart.Literal.forName(UNAMED));
            runnables = stream(instances)
                    .filter(i -> beanToTaskName(i).equalsIgnoreCase(taskName))
                    .collect(Collectors.toSet());
        }

        for (Runnable runnable : runnables) {
            runnable.run();
        }
    }

    public void exitIfNeeded() {
        //The tcks CustomConverTest is broken: It always returns true for boolean values.
        //To workaround this issue, we need to check if `init-and-exit` is explicitly defined.
        boolean initAndExitConfigured = propertyConfigured(QUARKUS_INIT_AND_EXIT);
        if (initAndExitConfigured && config.getValue().initAndExit) {
            preventFurtherRecorderSteps(5, "Error attempting to gracefully shutdown after initialization",
                    () -> new PreventFurtherStepsException("Gracefully exiting after initialization.", 0));
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

    private static String beanToTaskName(Runnable runnable) {
        //Get class name, strip prefix of generated runnables and hyphenate
        //We also strip everything that follows the _GeneratedRunnable as it may include addtional suffixes (e.g. _ClientProxy).
        return StringUtil.hyphenate(runnable.getClass().getSimpleName().replaceAll("_GeneratedRunnable.*", ""));
    }

    private static String propertyToEnvVar(String property) {
        return io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores(property).toUpperCase();
    }

    private static boolean propertyConfigured(String property) {
        return StreamSupport.stream(ConfigProvider.getConfig().getPropertyNames().spliterator(), false)
                .anyMatch(n -> property.equals(n) || propertyToEnvVar(property).equals(n));
    }

    private static Stream<Runnable> stream(InjectableInstance<Runnable> instance) {
        Spliterator<Runnable> spliterator = Spliterators.spliteratorUnknownSize(instance.iterator(), Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }
}
