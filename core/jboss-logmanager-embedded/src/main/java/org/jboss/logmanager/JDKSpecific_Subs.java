/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
