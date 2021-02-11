package io.quarkus.jaeger.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.spi.Reporter;

@Substitute
@TargetClass(className = "io.jaegertracing.internal.reporters.LoggingReporter")
final public class Target_LoggingReporter implements Reporter {

    @Substitute
    public Target_LoggingReporter() {

    }

    @Substitute
    @Override
    public void report(JaegerSpan span) {
        System.err.println("--- not logging: " + span);
    }

    @Substitute
    @Override
    public void close() {

    }

}
