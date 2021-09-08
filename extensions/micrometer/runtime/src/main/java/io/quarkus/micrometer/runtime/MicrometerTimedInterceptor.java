package io.quarkus.micrometer.runtime;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;

import org.jboss.logging.Logger;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.quarkus.arc.ArcInvocationContext;

/**
 * Quarkus defined interceptor for types or methods annotated with {@link Timed @Timed}.
 */
@Interceptor
@Timed
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 10)
public class MicrometerTimedInterceptor {

    private static final Logger log = Logger.getLogger(MicrometerTimedInterceptor.class);
    public static final String DEFAULT_METRIC_NAME = "method.timed";

    private final MeterRegistry meterRegistry;

    public MicrometerTimedInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @AroundInvoke
    Object timedMethod(ArcInvocationContext context) throws Exception {
        final boolean stopWhenCompleted = CompletionStage.class.isAssignableFrom(context.getMethod().getReturnType());
        final List<Sample> samples = getSamples(context);

        if (samples.isEmpty()) {
            // This should never happen - at least one @Timed binding must be present  
            return context.proceed();
        }

        if (stopWhenCompleted) {
            try {
                return ((CompletionStage<?>) context.proceed()).whenComplete((result, throwable) -> {
                    for (Sample sample : samples) {
                        sample.stop(MicrometerRecorder.getExceptionTag(throwable));
                    }
                });
            } catch (Exception ex) {
                for (Sample sample : samples) {
                    sample.stop(MicrometerRecorder.getExceptionTag(ex));
                }
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
            for (Sample sample : samples) {
                sample.stop(exceptionClass);
            }
        }
    }

    private List<Sample> getSamples(ArcInvocationContext context) {
        Method method = context.getMethod();
        Tags commonTags = getCommonTags(method.getDeclaringClass().getName(), method.getName());
        List<Timed> timed = context.findIterceptorBindings(Timed.class);
        if (timed.isEmpty()) {
            return Collections.emptyList();
        }
        List<Sample> samples = new ArrayList<>(timed.size());
        for (Timed t : timed) {
            if (t.longTask()) {
                samples.add(new LongTimerSample(t, commonTags));
            } else {
                samples.add(new TimerSample(t, commonTags));
            }
        }
        return samples;
    }

    private void record(Timed timed, Timer.Sample sample, String exceptionClass, Tags timerTags) {
        final String metricName = timed.value().isEmpty() ? DEFAULT_METRIC_NAME : timed.value();
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

    LongTaskTimer.Sample startLongTaskTimer(Timed timed, Tags commonTags, String metricName) {
        try {
            // This will throw if the annotation is incorrect.
            // Errors are checked for at build time, but ...
            return LongTaskTimer.builder(metricName)
                    .description(timed.description().isEmpty() ? null : timed.description())
                    .tags(commonTags)
                    .tags(timed.extraTags())
                    .publishPercentileHistogram(timed.histogram())
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

    abstract class Sample {

        protected final Timed timed;
        protected final Tags commonTags;

        public Sample(Timed timed, Tags commonTags) {
            this.timed = timed;
            this.commonTags = commonTags;
        }

        String metricName() {
            return timed.value().isEmpty() ? DEFAULT_METRIC_NAME : timed.value();
        }

        abstract void stop(String exceptionClass);
    }

    final class TimerSample extends Sample {

        private final Timer.Sample sample;

        public TimerSample(Timed timed, Tags commonTags) {
            super(timed, commonTags);
            this.sample = Timer.start(meterRegistry);
        }

        @Override
        void stop(String exceptionClass) {
            record(timed, sample, exceptionClass, Tags.concat(commonTags, timed.extraTags()));
        }

    }

    final class LongTimerSample extends Sample {

        private final LongTaskTimer.Sample sample;

        public LongTimerSample(Timed timed, Tags commonTags) {
            super(timed, commonTags);
            this.sample = startLongTaskTimer(timed, commonTags, metricName());
        }

        @Override
        void stop(String exceptionClass) {
            stopLongTaskTimer(metricName(), sample);
        }

    }
}
