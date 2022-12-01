package io.quarkus.jaeger.runtime.graal;

import org.jboss.logging.Logger;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.spi.Reporter;

@Substitute
@TargetClass(className = "io.jaegertracing.internal.reporters.LoggingReporter")
final public class Target_LoggingReporter implements Reporter {

    private static final Logger LOG = Logger.getLogger(Target_LoggingReporter.class);

    @Substitute
    public Target_LoggingReporter() {

    }

    @Substitute
    @Override
    public void report(JaegerSpan span) {
        LOG.infof("Span reported: %s", span);
    }

    @Substitute
    @Override
    public void close() {

    }

}
