package io.quarkus.micrometer.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;

import org.jboss.logging.Logger;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.quarkus.arc.ArcInvocationContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Functions;

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
    private final MeterTagsSupport meterTagsSupport;
    private final Map<String, Timer.Builder> methodsBuilders;
    private final Map<String, LongTaskTimer.Sample> longMethodsBuilders;

    public MicrometerTimedInterceptor(MeterRegistry meterRegistry, MeterTagsSupport meterTagsSupport) {
        this.meterRegistry = meterRegistry;
        this.meterTagsSupport = meterTagsSupport;
        this.methodsBuilders = new HashMap<>();
        this.longMethodsBuilders = new HashMap<>();
    }

    @AroundInvoke
    @SuppressWarnings("unchecked")
    Object timedMethod(ArcInvocationContext context) throws Exception {
        final List<Sample> samples = getSamples(context);

        if (samples.isEmpty()) {
            // This should never happen - at least one @Timed binding must be present
            return context.proceed();
        }

        Class<?> returnType = context.getMethod().getReturnType();
        if (TypesUtil.isCompletionStage(returnType)) {
            try {
                return ((CompletionStage<?>) context.proceed()).whenComplete((result, throwable) -> {
                    stop(samples, MicrometerRecorder.getExceptionTag(throwable));
                });
            } catch (Exception ex) {
                stop(samples, MicrometerRecorder.getExceptionTag(ex));
                throw ex;
            }
        } else if (TypesUtil.isUni(returnType)) {
            try {
                return ((Uni<Object>) context.proceed()).onTermination().invoke(
                        new Functions.TriConsumer<>() {
                            @Override
                            public void accept(Object o, Throwable throwable, Boolean cancelled) {
                                stop(samples, MicrometerRecorder.getExceptionTag(throwable));
                            }
                        });
            } catch (Exception ex) {
                stop(samples, MicrometerRecorder.getExceptionTag(ex));
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
            stop(samples, exceptionClass);
        }
    }

    private List<Sample> getSamples(ArcInvocationContext context) {
        Set<Timed> timed = context.getInterceptorBindings(Timed.class);
        if (timed.isEmpty()) {
            return Collections.emptyList();
        }
        Tags tags = meterTagsSupport.getTags(context);
        List<Sample> samples = new ArrayList<>(timed.size());
        for (Timed t : timed) {
            if (t.longTask()) {
                samples.add(new LongTimerSample(t, tags));
            } else {
                samples.add(new TimerSample(t, tags));
            }
        }
        return samples;
    }

    private void stop(List<Sample> samples, String throwableClassName) {
        for (Sample sample : samples) {
            sample.stop(throwableClassName);
        }
    }

    private void record(Timed timed, Timer.Sample sample, String exceptionClass, Tags timerTags) {
        final String metricName = timed.value().isEmpty() ? DEFAULT_METRIC_NAME : timed.value();
        try {
            Timer.Builder builder = methodsBuilders.computeIfAbsent(metricName, new Function<String, Timer.Builder>() {
                @Override
                public Timer.Builder apply(String t) {
                    return Timer.builder(metricName)
                            .description(timed.description().isEmpty() ? null : timed.description())
                            .tags(timerTags)
                            .tag("exception", exceptionClass)
                            .publishPercentileHistogram(timed.histogram())
                            .publishPercentiles(timed.percentiles().length == 0 ? null : timed.percentiles());
                }
            });

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
            return longMethodsBuilders.computeIfAbsent(metricName, new Function<String, LongTaskTimer.Sample>() {
                @Override
                public LongTaskTimer.Sample apply(String t) {
                    return LongTaskTimer.builder(metricName)
                            .description(timed.description().isEmpty() ? null : timed.description())
                            .tags(commonTags)
                            .tags(timed.extraTags())
                            .publishPercentileHistogram(timed.histogram())
                            .register(meterRegistry)
                            .start();
                }
            });
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

    abstract static class Sample {

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
