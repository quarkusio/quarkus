package org.jboss.logmanager;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 *
 */
@TargetClass(JDKSpecific.class)
@Substitute
final class JDKSpecific_Subs {

    @TargetClass(JDKSpecific.Gateway.class)
    @Delete
    static final class Gateway {}

    @TargetClass(JDKSpecific.GatewayPrivilegedAction.class)
    @Delete
    static final class GatewayPrivilegedAction {}

    @Substitute
    static void calculateCaller(ExtLogRecord logRecord) {
        final String loggerClassName = logRecord.getLoggerClassName();
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        boolean found = false;
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().equals(loggerClassName)) {
                // next entry could be the one we want!
                found = true;
            } else {
                if (found) {
                    logRecord.setSourceClassName(element.getClassName());
                    logRecord.setSourceMethodName(element.getMethodName());
                    logRecord.setSourceFileName(element.getFileName());
                    logRecord.setSourceLineNumber(element.getLineNumber());
                    return;
                }
            }
        }
        logRecord.setUnknownCaller();
        return;
    }
}
