package io.quarkus.logging.gelf.graal;

import java.util.logging.LogRecord;

import org.jboss.logmanager.ExtLogRecord;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "biz.paluch.logging.gelf.jul.JulLogEvent")
public final class JulLogEventSubstitution {

    @Substitute
    private String getThreadName(LogRecord record) {
        // This is a temporary substitution to avoid using JMX to retrieve the name of the thread
        // see https://github.com/mp911de/logstash-gelf/pull/214 and https://github.com/mp911de/logstash-gelf/pull/217

        return ((ExtLogRecord) record).getThreadName();
    }
}
