package io.quarkus.runtime.graal;

import java.util.logging.Handler;

import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.handlers.DelayedHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.runtime.logging.InitialConfigurator;

/**
 */
@TargetClass(className = "org.jboss.logmanager.LoggerNode")
final class Target_org_jboss_logmanager_LoggerNode {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    volatile Handler[] handlers;
}

@TargetClass(className = "org.slf4j.LoggerFactory")
final class Target_org_slf4j_LoggerFactory {

    @Substitute
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz.getName());
    }

}

@TargetClass(InitialConfigurator.class)
final class Target_io_quarkus_runtime_logging_InitialConfigurator {
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    @Alias
    public static DelayedHandler DELAYED_HANDLER = new DelayedHandler();
}

@TargetClass(java.util.logging.Logger.class)
final class Target_java_util_logging_Logger {
    @Substitute
    static java.util.logging.Logger getLogger(String name) {
        return LogContext.getLogContext().getLogger(name);
    }

    @Substitute
    static java.util.logging.Logger getLogger(String name, String ignored) {
        return LogContext.getLogContext().getLogger(name);
    }
}

final class LoggingSubstitutions {
}
