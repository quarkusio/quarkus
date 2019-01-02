/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

import static java.security.AccessController.doPrivileged;

import java.lang.module.ModuleDescriptor;
import java.security.PrivilegedAction;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jboss.modules.Module;
import org.jboss.modules.Version;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class JDKSpecific {

    static final StackWalker WALKER = doPrivileged(new GetStackWalkerAction());

    private JDKSpecific() {}

    private static final boolean JBOSS_MODULES;

    static {
        boolean jbossModules = false;
        try {
            Module.getStartTime();
            jbossModules = true;
        } catch (Throwable ignored) {}
        JBOSS_MODULES = jbossModules;
    }

    static void calculateCaller(ExtLogRecord logRecord) {
        WALKER.walk(new CallerCalcFunction(logRecord));
    }

    static void calculateJdkModule(final ExtLogRecord logRecord, final Class<?> clazz) {
        final java.lang.Module module = clazz.getModule();
        if (module != null) {
            logRecord.setSourceModuleName(module.getName());
            final ModuleDescriptor descriptor = module.getDescriptor();
            if (descriptor != null) {
                final Optional<ModuleDescriptor.Version> optional = descriptor.version();
                if (optional.isPresent()) {
                    logRecord.setSourceModuleVersion(optional.get().toString());
                } else {
                    logRecord.setSourceModuleVersion(null);
                }
            }
        }
    }

    static void calculateModule(final ExtLogRecord logRecord, final Class<?> clazz) {
        final Module module = Module.forClass(clazz);
        if (module != null) {
            logRecord.setSourceModuleName(module.getName());
            final Version version = module.getVersion();
            if (version != null) {
                logRecord.setSourceModuleVersion(version.toString());
            } else {
                logRecord.setSourceModuleVersion(null);
            }
        } else {
            calculateJdkModule(logRecord, clazz);
        }
    }

    static final class CallerCalcFunction implements Function<Stream<StackWalker.StackFrame>, Void> {
        private final ExtLogRecord logRecord;

        CallerCalcFunction(final ExtLogRecord logRecord) {
            this.logRecord = logRecord;
        }

        public Void apply(final Stream<StackWalker.StackFrame> stream) {
            final String loggerClassName = logRecord.getLoggerClassName();
            final Iterator<StackWalker.StackFrame> iterator = stream.iterator();
            boolean found = false;
            while (iterator.hasNext()) {
                final StackWalker.StackFrame frame = iterator.next();
                final Class<?> clazz = frame.getDeclaringClass();
                if (clazz.getName().equals(loggerClassName)) {
                    // next entry could be the one we want!
                    found = true;
                } else if (found) {
                    logRecord.setSourceClassName(frame.getClassName());
                    logRecord.setSourceMethodName(frame.getMethodName());
                    logRecord.setSourceFileName(frame.getFileName());
                    logRecord.setSourceLineNumber(frame.getLineNumber());
                    if (JBOSS_MODULES) {
                        calculateModule(logRecord, clazz);
                    } else {
                        calculateJdkModule(logRecord, clazz);
                    }
                    return null;
                }
            }
            logRecord.setUnknownCaller();
            return null;
        }
    }

    static final class GetStackWalkerAction implements PrivilegedAction<StackWalker> {
        GetStackWalkerAction() {}

        public StackWalker run() {
            return StackWalker.getInstance(EnumSet.of(StackWalker.Option.RETAIN_CLASS_REFERENCE));
        }
    }

}
