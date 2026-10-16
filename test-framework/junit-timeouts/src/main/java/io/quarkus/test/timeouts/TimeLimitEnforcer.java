package io.quarkus.test.timeouts;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Enforces {@link TimeLimit} on the test thread: started right before a test method runs and finished right after.
 * <p>
 * This is done with callbacks rather than with an {@code InvocationInterceptor} because the Quarkus test extensions
 * invoke the test method themselves, which would skip an interceptor registered after them.
 */
final class TimeLimitEnforcer {

    private static final ScheduledThreadPoolExecutor SCHEDULER = new ScheduledThreadPoolExecutor(1, runnable -> {
        Thread thread = new Thread(runnable, "test-time-limit");
        thread.setDaemon(true);
        return thread;
    });

    static {
        SCHEDULER.setRemoveOnCancelPolicy(true);
    }

    private final Thread testThread;
    private final Duration timeout;
    private final String description;
    private final AtomicBoolean expired = new AtomicBoolean();
    private final ScheduledFuture<?> interrupter;

    private TimeLimitEnforcer(Duration timeout, String description) {
        this.testThread = Thread.currentThread();
        this.timeout = timeout;
        this.description = description;
        this.interrupter = SCHEDULER.schedule(() -> {
            expired.set(true);
            testThread.interrupt();
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * @return an enforcer for the running test, or null if the test has no {@link TimeLimit}
     */
    static TimeLimitEnforcer startFor(ExtensionContext context) {
        int multiplier = findMultiplier(context);
        if (multiplier <= 0) {
            return null;
        }
        return start(TestTimeouts.get().multipliedBy(multiplier), context.getRequiredTestMethod().getName() + "()");
    }

    static TimeLimitEnforcer start(Duration timeout, String description) {
        return new TimeLimitEnforcer(timeout, description);
    }

    /**
     * Stops the timer, and fails with a {@link TimeoutException} if the limit was reached in the meantime.
     */
    void finish() throws TimeoutException {
        interrupter.cancel(false);
        if (expired.get()) {
            // do not leak the interrupt to the next test
            Thread.interrupted();
            throw new TimeoutException(description + " timed out after " + timeout.toMillis() + " ms");
        }
    }

    /**
     * Annotations are matched by name and read reflectively: for Quarkus tests the test class is loaded by the
     * application classloader, so its {@code TimeLimit} is not the same class as the one seen by this extension.
     *
     * @return the multiplier of the closest {@link TimeLimit}, or 0 if there is none
     */
    private static int findMultiplier(ExtensionContext context) {
        int onMethod = multiplierOf(context.getRequiredTestMethod());
        if (onMethod > 0) {
            return onMethod;
        }
        for (Class<?> outer = context.getRequiredTestClass(); outer != null; outer = outer.getEnclosingClass()) {
            for (Class<?> c = outer; c != null; c = c.getSuperclass()) {
                int onClass = multiplierOf(c);
                if (onClass > 0) {
                    return onClass;
                }
            }
        }
        return 0;
    }

    private static int multiplierOf(AnnotatedElement element) {
        for (Annotation annotation : element.getAnnotations()) {
            if (annotation.annotationType().getName().equals(TimeLimit.class.getName())) {
                try {
                    return (Integer) annotation.annotationType().getMethod("multiplier").invoke(annotation);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot read " + annotation, e);
                }
            }
        }
        return 0;
    }
}
