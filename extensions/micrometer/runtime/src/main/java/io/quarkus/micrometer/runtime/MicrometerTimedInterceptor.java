package io.quarkus.micrometer.runtime;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

/**
 * Quarkus defined interceptor for types or methods annotated with {@link Timed @Timed}.
 *
 * @see Timed
 */
@Interceptor
@MicrometerTimed
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 10)
public class MicrometerTimedInterceptor {
    private static final Logger log = Logger.getLogger(MicrometerTimedInterceptor.class);
    public static final String DEFAULT_METRIC_NAME = "method.timed";

    private final MeterRegistry meterRegistry;

    public MicrometerTimedInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @AroundInvoke
    Object timedMethod(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        Timed timed = method.getAnnotation(Timed.class);
        if (timed == null) {
            return context.proceed();
        }

        Tags commonTags = getCommonTags(method.getDeclaringClass().getName(), method.getName());
        final boolean stopWhenCompleted = CompletionStage.class.isAssignableFrom(method.getReturnType());

        return time(context, timed, commonTags, stopWhenCompleted);
    }

    Object time(InvocationContext context, Timed timed, Tags commonTags, boolean stopWhenCompleted) throws Exception {
        final String metricName = timed.value().isEmpty() ? DEFAULT_METRIC_NAME : timed.value();

        if (timed.longTask()) {
            return processWithLongTaskTimer(context, timed, commonTags, metricName, stopWhenCompleted);
        } else {
            return processWithTimer(context, timed, commonTags, metricName, stopWhenCompleted);
        }
    }

    private Object processWithTimer(InvocationContext context, Timed timed, Tags commonTags, String metricName,
            boolean stopWhenCompleted) throws Exception {

        Timer.Sample sample = Timer.start(meterRegistry);
        Tags timerTags = Tags.concat(commonTags, timed.extraTags());

        if (stopWhenCompleted) {
            try {
                return ((CompletionStage<?>) context.proceed()).whenComplete((result, throwable) -> {
                    record(timed, metricName, sample, MicrometerRecorder.getExceptionTag(throwable), timerTags);
                });
            } catch (Exception ex) {
                record(timed, metricName, sample, MicrometerRecorder.getExceptionTag(ex), timerTags);
                throw ex;
            }
        }

        String exceptionClass = MicrometerRecorder.getExceptionTag(null);
        try {
            return context.proceed();
        } catch (Exception ex) {
            exceptionClass = MicrometerRecorder.getExceptionTag(ex);
            throw ex;
        } finally {
            record(timed, metricName, sample, exceptionClass, timerTags);
        }
    }

    private void record(Timed timed, String metricName, Timer.Sample sample, String exceptionClass, Tags timerTags) {
        try {
            Timer.Builder builder = Timer.builder(metricName)
                    .description(timed.description().isEmpty() ? null : timed.description())
                    .tags(timerTags)
                    .tag("exception", exceptionClass)
                    .publishPercentileHistogram(timed.histogram())
                    .publishPercentiles(timed.percentiles().length == 0 ? null : timed.percentiles());

            sample.stop(builder.register(meterRegistry));
        } catch (Exception e) {
            // ignoring on purpose: possible meter registration error should not interrupt main code flow.
            log.warnf(e, "Unable to record observed timer value for %s with exceptionClass %s",
                    metricName, exceptionClass);
        }
    }

    private Object processWithLongTaskTimer(InvocationContext context, Timed timed, Tags commonTags, String metricName,
            boolean stopWhenCompleted) throws Exception {
        LongTaskTimer.Sample sample = startLongTaskTimer(timed, commonTags, metricName);
        if (sample == null) {
            return context.proceed();
        }

        if (stopWhenCompleted) {
            try {
                return ((CompletionStage<?>) context.proceed())
                        .whenComplete((result, throwable) -> stopLongTaskTimer(metricName, sample));
            } catch (Exception ex) {
                stopLongTaskTimer(metricName, sample);
                throw ex;
            }
        }

        try {
            return context.proceed();
        } finally {
            stopLongTaskTimer(metricName, sample);
        }
    }

    LongTaskTimer.Sample startLongTaskTimer(Timed timed, Tags commonTags, String metricName) {
        try {
            // This will throw if the annotation is incorrect.
            // Errors are checked for at build time, but ...
            return LongTaskTimer.builder(metricName)
                    .description(timed.description().isEmpty() ? null : timed.description())
                    .tags(commonTags)
                    .tags(timed.extraTags())
                    .register(meterRegistry)
                    .start();
        } catch (Exception e) {
            // ignoring on purpose: possible meter registration error should not interrupt main code flow.
            log.warnf(e, "Unable to create long task timer named %s", metricName);
            return null;
        }
    }

    private void stopLongTaskTimer(String metricName, LongTaskTimer.Sample sample) {
        try {
            sample.stop();
        } catch (Exception e) {
            // ignoring on purpose
            log.warnf(e, "Unable to update long task timer named %s", metricName);
        }
    }

    private Tags getCommonTags(String className, String methodName) {
        return Tags.of("class", className, "method", methodName);
    }
}
