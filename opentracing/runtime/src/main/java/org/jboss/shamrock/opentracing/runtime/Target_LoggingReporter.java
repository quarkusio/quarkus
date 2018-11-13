package org.jboss.shamrock.opentracing.runtime;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.uber.jaeger.Span;
import com.uber.jaeger.reporters.Reporter;

@Substitute
@TargetClass(className = "com.uber.jaeger.reporters.LoggingReporter")
final public class Target_LoggingReporter implements Reporter {

    @Substitute
    public Target_LoggingReporter() {

    }

    @Substitute
    @Override
    public void report(Span span) {
        System.err.println( "--- not logging: " + span);
    }

    @Substitute
    @Override
    public void close() {

    }
}
