package io.quarkus.virtual.threads;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

class VirtualThreadExecutorSupplierTest {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_20, disabledReason = "Virtual Threads are a preview feature starting from Java 20")
    void virtualThreadCustomScheduler()
            throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Executor executor = VirtualThreadsRecorder.newVirtualThreadPerTaskExecutorWithName("vthread-");
        var assertSubscriber = Uni.createFrom().emitter(e -> {
            assertThat(Thread.currentThread().getName()).isNotEmpty()
                    .startsWith("vthread-");
            assertThatItRunsOnVirtualThread();
            e.complete(null);
        }).runSubscriptionOn(executor)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertSubscriber.awaitItem(Duration.ofSeconds(1)).assertCompleted();
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_20, disabledReason = "Virtual Threads are a preview feature starting from Java 20")
    void execute() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Executor executor = VirtualThreadsRecorder.newVirtualThreadPerTaskExecutor();
        var assertSubscriber = Uni.createFrom().emitter(e -> {
            assertThat(Thread.currentThread().getName()).isEmpty();
            assertThatItRunsOnVirtualThread();
            e.complete(null);
        }).runSubscriptionOn(executor)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        assertSubscriber.awaitItem(Duration.ofSeconds(1)).assertCompleted();
    }

    public static void assertThatItRunsOnVirtualThread() {
        // We cannot depend on a Java 20.
        try {
            Method isVirtual = Thread.class.getMethod("isVirtual");
            isVirtual.setAccessible(true);
            boolean virtual = (Boolean) isVirtual.invoke(Thread.currentThread());
            if (!virtual) {
                throw new AssertionError("Thread " + Thread.currentThread() + " is not a virtual thread");
            }
        } catch (Exception e) {
            throw new AssertionError(
                    "Thread " + Thread.currentThread() + " is not a virtual thread - cannot invoke Thread.isVirtual()", e);
        }
    }
}
