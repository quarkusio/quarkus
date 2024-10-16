package io.quarkus.opentelemetry.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.opentelemetry.api.incubator.events.EventLoggerProvider;

public class Substitutions {

    @TargetClass(className = "io.opentelemetry.api.incubator.events.GlobalEventLoggerProvider")
    static final class Target_GlobalEventEmitterProvider {
        @Substitute
        public static void set(EventLoggerProvider eventEmitterProvider) {
            // do nothing. We don't support events yet. Default is EventEmitterProvider.noop()
        }
    }
}
