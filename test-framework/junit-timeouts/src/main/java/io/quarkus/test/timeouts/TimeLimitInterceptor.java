package io.quarkus.test.timeouts;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Enforces {@link TimeLimit} on test methods.
 */
final class TimeLimitInterceptor implements InvocationInterceptor {

    private static final ScheduledThreadPoolExecutor SCHEDULER = new ScheduledThreadPoolExecutor(1, runnable -> {
        Thread thread = new Thread(runnable, "test-time-limit");
        thread.setDaemon(true);
        return thread;
    });

    static {
        SCHEDULER.setRemoveOnCancelPolicy(true);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        intercept(invocation, extensionContext);
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        intercept(invocation, extensionContext);
    }

    private static void intercept(Invocation<Void> invocation, ExtensionContext context) throws Throwable {
        TimeLimit limit = findLimit(context);
        if (limit == null) {
            invocation.proceed();
            return;
        }
        Duration timeout = TestTimeouts.get().multipliedBy(limit.multiplier());
        callWithTimeLimit(timeout, context.getRequiredTestMethod().getName() + "()", invocation);
    }

    private static TimeLimit findLimit(ExtensionContext context) {
        TimeLimit onMethod = AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), TimeLimit.class)
                .orElse(null);
        if (onMethod != null) {
            return onMethod;
        }
        for (Class<?> c = context.getRequiredTestClass(); c != null; c = c.getEnclosingClass()) {
            TimeLimit onClass = AnnotationSupport.findAnnotation((AnnotatedElement) c, TimeLimit.class).orElse(null);
            if (onClass != null) {
                return onClass;
            }
        }
        return null;
    }

    /**
     * Runs the invocation on the current thread, interrupting it if it takes longer than the timeout.
     */
    static <T> T callWithTimeLimit(Duration timeout, String description, Invocation<T> invocation) throws Throwable {
        Thread testThread = Thread.currentThread();
        AtomicBoolean expired = new AtomicBoolean();
        ScheduledFuture<?> interrupter = SCHEDULER.schedule(() -> {
            expired.set(true);
            testThread.interrupt();
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        T result;
        try {
            result = invocation.proceed();
        } catch (Throwable t) {
            interrupter.cancel(false);
            if (expired.get()) {
                Thread.interrupted();
                TimeoutException timeoutException = timeoutException(description, timeout);
                timeoutException.addSuppressed(t);
                throw timeoutException;
            }
            throw t;
        }
        interrupter.cancel(false);
        if (expired.get()) {
            Thread.interrupted();
            throw timeoutException(description, timeout);
        }
        return result;
    }

    private static TimeoutException timeoutException(String description, Duration timeout) {
        return new TimeoutException(description + " timed out after " + timeout.toMillis() + " ms");
    }
}
