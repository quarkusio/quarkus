/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2011 Red Hat, Inc., and individual contributors
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

package org.jboss.logging;

import com.oracle.svm.core.annotate.AlwaysInline;

import java.io.Serializable;

/**
 * A serializable, delegating basic logger instance.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class DelegatingBasicLogger implements BasicLogger, Serializable {

    private static final long serialVersionUID = -5774903162389601853L;

    /**
     * The cached logger class name.
     */
    private static final String FQCN = DelegatingBasicLogger.class.getName();

    /**
     * The delegate logger.
     */
    protected final Logger log;

    /**
     * Construct a new instance.
     *
     * @param log the logger to which calls should be delegated
     */
    public DelegatingBasicLogger(final Logger log) {
        this.log = log;
    }

    @Override
    @AlwaysInline("Fast level checks")
    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void trace(final Object message) {
        log.trace(FQCN, message, null);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void trace(final Object message, final Throwable t) {
        log.trace(FQCN, message, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void trace(final String loggerFqcn, final Object message, final Throwable t) {
        log.trace(loggerFqcn, message, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void trace(final String loggerFqcn, final Object message, final Object[] params, final Throwable t) {
        log.trace(loggerFqcn, message, params, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracev(final String format, final Object... params) {
        log.logv(FQCN, Logger.Level.TRACE, null, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracev(final String format, final Object param1) {
        log.logv(FQCN, Logger.Level.TRACE, null, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracev(final String format, final Object param1, final Object param2) {
        log.logv(FQCN, Logger.Level.TRACE, null, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracev(final String format, final Object param1, final Object param2, final Object param3) {
        log.logv(FQCN, Logger.Level.TRACE, null, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracev(final Throwable t, final String format, final Object... params) {
        log.logv(FQCN, Logger.Level.TRACE, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracev(final Throwable t, final String format, final Object param1) {
        log.logv(FQCN, Logger.Level.TRACE, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracev(final Throwable t, final String format, final Object param1, final Object param2) {
        log.logv(FQCN, Logger.Level.TRACE, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracev(final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logv(FQCN, Logger.Level.TRACE, t, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final Object... params) {
        log.logf(FQCN, Logger.Level.TRACE, null, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final Object param1) {
        log.logf(FQCN, Logger.Level.TRACE, null, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final Object param1, final Object param2) {
        log.logf(FQCN, Logger.Level.TRACE, null, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final Object param1, final Object param2, final Object param3) {
        log.logf(FQCN, Logger.Level.TRACE, null, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final Object... params) {
        log.logf(FQCN, Logger.Level.TRACE, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final Object param1) {
        log.logf(FQCN, Logger.Level.TRACE, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final Object param1, final Object param2) {
        log.logf(FQCN, Logger.Level.TRACE, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logf(FQCN, Logger.Level.TRACE, t, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final int arg) {
        log.tracef(format, arg);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final int arg1, final int arg2) {
        log.tracef(format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final int arg1, final Object arg2) {
        log.tracef(format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final int arg1, final int arg2, final int arg3) {
        log.tracef(format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final int arg1, final int arg2, final Object arg3) {
        log.tracef(format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final int arg1, final Object arg2, final Object arg3) {
        log.tracef(format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final int arg) {
        log.tracef(t, format, arg);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final int arg1, final int arg2) {
        log.tracef(t, format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final int arg1, final Object arg2) {
        log.tracef(t, format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final int arg1, final int arg2, final int arg3) {
        log.tracef(t, format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final int arg1, final int arg2, final Object arg3) {
        log.tracef(t, format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final int arg1, final Object arg2, final Object arg3) {
        log.tracef(t, format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final long arg) {
        log.tracef(format, arg);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final long arg1, final long arg2) {
        log.tracef(format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final long arg1, final Object arg2) {
        log.tracef(format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final long arg1, final long arg2, final long arg3) {
        log.tracef(format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final long arg1, final long arg2, final Object arg3) {
        log.tracef(format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final long arg1, final Object arg2, final Object arg3) {
        log.tracef(format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final long arg) {
        log.tracef(t, format, arg);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final long arg1, final long arg2) {
        log.tracef(t, format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final long arg1, final Object arg2) {
        log.tracef(t, format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final long arg1, final long arg2, final long arg3) {
        log.tracef(t, format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final long arg1, final long arg2, final Object arg3) {
        log.tracef(t, format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final long arg1, final Object arg2, final Object arg3) {
        log.tracef(t, format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debug(final Object message) {
        log.debug(FQCN, message, null);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debug(final Object message, final Throwable t) {
        log.debug(FQCN, message, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debug(final String loggerFqcn, final Object message, final Throwable t) {
        log.debug(loggerFqcn, message, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debug(final String loggerFqcn, final Object message, final Object[] params, final Throwable t) {
        log.debug(loggerFqcn, message, params, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugv(final String format, final Object... params) {
        log.logv(FQCN, Logger.Level.DEBUG, null, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugv(final String format, final Object param1) {
        log.logv(FQCN, Logger.Level.DEBUG, null, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugv(final String format, final Object param1, final Object param2) {
        log.logv(FQCN, Logger.Level.DEBUG, null, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugv(final String format, final Object param1, final Object param2, final Object param3) {
        log.logv(FQCN, Logger.Level.DEBUG, null, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugv(final Throwable t, final String format, final Object... params) {
        log.logv(FQCN, Logger.Level.DEBUG, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugv(final Throwable t, final String format, final Object param1) {
        log.logv(FQCN, Logger.Level.DEBUG, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugv(final Throwable t, final String format, final Object param1, final Object param2) {
        log.logv(FQCN, Logger.Level.DEBUG, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugv(final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logv(FQCN, Logger.Level.DEBUG, t, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final Object... params) {
        log.logf(FQCN, Logger.Level.DEBUG, null, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final Object param1) {
        log.logf(FQCN, Logger.Level.DEBUG, null, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final Object param1, final Object param2) {
        log.logf(FQCN, Logger.Level.DEBUG, null, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final Object param1, final Object param2, final Object param3) {
        log.logf(FQCN, Logger.Level.DEBUG, null, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final Object... params) {
        log.logf(FQCN, Logger.Level.DEBUG, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final Object param1) {
        log.logf(FQCN, Logger.Level.DEBUG, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final Object param1, final Object param2) {
        log.logf(FQCN, Logger.Level.DEBUG, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logf(FQCN, Logger.Level.DEBUG, t, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final int arg) {
        log.debugf(format, arg);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final int arg1, final int arg2) {
        log.debugf(format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final int arg1, final Object arg2) {
        log.debugf(format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final int arg1, final int arg2, final int arg3) {
        log.debugf(format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final int arg1, final int arg2, final Object arg3) {
        log.debugf(format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final int arg1, final Object arg2, final Object arg3) {
        log.debugf(format, arg1, arg2, arg3);
    }

    @AlwaysInline("Fast level checks")
    @Override
    public void debugf(final Throwable t, final String format, final int arg) {
        log.debugf(t, format, arg);
    }

    @AlwaysInline("Fast level checks")
    @Override
    public void debugf(final Throwable t, final String format, final int arg1, final int arg2) {
        log.debugf(t, format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final int arg1, final Object arg2) {
        log.debugf(t, format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final int arg1, final int arg2, final int arg3) {
        log.debugf(t, format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final int arg1, final int arg2, final Object arg3) {
        log.debugf(t, format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final int arg1, final Object arg2, final Object arg3) {
        log.debugf(t, format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final long arg) {
        log.debugf(format, arg);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final long arg1, final long arg2) {
        log.debugf(format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final long arg1, final Object arg2) {
        log.debugf(format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final long arg1, final long arg2, final long arg3) {
        log.debugf(format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final long arg1, final long arg2, final Object arg3) {
        log.debugf(format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final long arg1, final Object arg2, final Object arg3) {
        log.debugf(format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final long arg) {
        log.debugf(t, format, arg);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final long arg1, final long arg2) {
        log.debugf(t, format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final long arg1, final Object arg2) {
        log.debugf(t, format, arg1, arg2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final long arg1, final long arg2, final long arg3) {
        log.debugf(t, format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final long arg1, final long arg2, final Object arg3) {
        log.debugf(t, format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final long arg1, final Object arg2, final Object arg3) {
        log.debugf(t, format, arg1, arg2, arg3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void info(final Object message) {
        log.info(FQCN, message, null);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void info(final Object message, final Throwable t) {
        log.info(FQCN, message, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void info(final String loggerFqcn, final Object message, final Throwable t) {
        log.info(loggerFqcn, message, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void info(final String loggerFqcn, final Object message, final Object[] params, final Throwable t) {
        log.info(loggerFqcn, message, params, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infov(final String format, final Object... params) {
        log.logv(FQCN, Logger.Level.INFO, null, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infov(final String format, final Object param1) {
        log.logv(FQCN, Logger.Level.INFO, null, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infov(final String format, final Object param1, final Object param2) {
        log.logv(FQCN, Logger.Level.INFO, null, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infov(final String format, final Object param1, final Object param2, final Object param3) {
        log.logv(FQCN, Logger.Level.INFO, null, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infov(final Throwable t, final String format, final Object... params) {
        log.logv(FQCN, Logger.Level.INFO, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infov(final Throwable t, final String format, final Object param1) {
        log.logv(FQCN, Logger.Level.INFO, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infov(final Throwable t, final String format, final Object param1, final Object param2) {
        log.logv(FQCN, Logger.Level.INFO, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infov(final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logv(FQCN, Logger.Level.INFO, t, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infof(final String format, final Object... params) {
        log.logf(FQCN, Logger.Level.INFO, null, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infof(final String format, final Object param1) {
        log.logf(FQCN, Logger.Level.INFO, null, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infof(final String format, final Object param1, final Object param2) {
        log.logf(FQCN, Logger.Level.INFO, null, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infof(final String format, final Object param1, final Object param2, final Object param3) {
        log.logf(FQCN, Logger.Level.INFO, null, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infof(final Throwable t, final String format, final Object... params) {
        log.logf(FQCN, Logger.Level.INFO, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infof(final Throwable t, final String format, final Object param1) {
        log.logf(FQCN, Logger.Level.INFO, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infof(final Throwable t, final String format, final Object param1, final Object param2) {
        log.logf(FQCN, Logger.Level.INFO, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void infof(final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logf(FQCN, Logger.Level.INFO, t, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warn(final Object message) {
        log.warn(FQCN, message, null);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warn(final Object message, final Throwable t) {
        log.warn(FQCN, message, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warn(final String loggerFqcn, final Object message, final Throwable t) {
        log.warn(loggerFqcn, message, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warn(final String loggerFqcn, final Object message, final Object[] params, final Throwable t) {
        log.warn(loggerFqcn, message, params, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnv(final String format, final Object... params) {
        log.logv(FQCN, Logger.Level.WARN, null, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnv(final String format, final Object param1) {
        log.logv(FQCN, Logger.Level.WARN, null, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnv(final String format, final Object param1, final Object param2) {
        log.logv(FQCN, Logger.Level.WARN, null, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnv(final String format, final Object param1, final Object param2, final Object param3) {
        log.logv(FQCN, Logger.Level.WARN, null, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnv(final Throwable t, final String format, final Object... params) {
        log.logv(FQCN, Logger.Level.WARN, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnv(final Throwable t, final String format, final Object param1) {
        log.logv(FQCN, Logger.Level.WARN, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnv(final Throwable t, final String format, final Object param1, final Object param2) {
        log.logv(FQCN, Logger.Level.WARN, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnv(final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logv(FQCN, Logger.Level.WARN, t, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnf(final String format, final Object... params) {
        log.logf(FQCN, Logger.Level.WARN, null, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnf(final String format, final Object param1) {
        log.logf(FQCN, Logger.Level.WARN, null, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnf(final String format, final Object param1, final Object param2) {
        log.logf(FQCN, Logger.Level.WARN, null, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnf(final String format, final Object param1, final Object param2, final Object param3) {
        log.logf(FQCN, Logger.Level.WARN, null, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnf(final Throwable t, final String format, final Object... params) {
        log.logf(FQCN, Logger.Level.WARN, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnf(final Throwable t, final String format, final Object param1) {
        log.logf(FQCN, Logger.Level.WARN, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnf(final Throwable t, final String format, final Object param1, final Object param2) {
        log.logf(FQCN, Logger.Level.WARN, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void warnf(final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logf(FQCN, Logger.Level.WARN, t, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void error(final Object message) {
        log.error(FQCN, message, null);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void error(final Object message, final Throwable t) {
        log.error(FQCN, message, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void error(final String loggerFqcn, final Object message, final Throwable t) {
        log.error(loggerFqcn, message, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void error(final String loggerFqcn, final Object message, final Object[] params, final Throwable t) {
        log.error(loggerFqcn, message, params, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorv(final String format, final Object... params) {
        log.logv(FQCN, Logger.Level.ERROR, null, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorv(final String format, final Object param1) {
        log.logv(FQCN, Logger.Level.ERROR, null, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorv(final String format, final Object param1, final Object param2) {
        log.logv(FQCN, Logger.Level.ERROR, null, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorv(final String format, final Object param1, final Object param2, final Object param3) {
        log.logv(FQCN, Logger.Level.ERROR, null, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorv(final Throwable t, final String format, final Object... params) {
        log.logv(FQCN, Logger.Level.ERROR, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorv(final Throwable t, final String format, final Object param1) {
        log.logv(FQCN, Logger.Level.ERROR, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorv(final Throwable t, final String format, final Object param1, final Object param2) {
        log.logv(FQCN, Logger.Level.ERROR, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorv(final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logv(FQCN, Logger.Level.ERROR, t, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorf(final String format, final Object... params) {
        log.logf(FQCN, Logger.Level.ERROR, null, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorf(final String format, final Object param1) {
        log.logf(FQCN, Logger.Level.ERROR, null, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorf(final String format, final Object param1, final Object param2) {
        log.logf(FQCN, Logger.Level.ERROR, null, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorf(final String format, final Object param1, final Object param2, final Object param3) {
        log.logf(FQCN, Logger.Level.ERROR, null, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorf(final Throwable t, final String format, final Object... params) {
        log.logf(FQCN, Logger.Level.ERROR, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorf(final Throwable t, final String format, final Object param1) {
        log.logf(FQCN, Logger.Level.ERROR, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorf(final Throwable t, final String format, final Object param1, final Object param2) {
        log.logf(FQCN, Logger.Level.ERROR, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void errorf(final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logf(FQCN, Logger.Level.ERROR, t, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatal(final Object message) {
        log.fatal(FQCN, message, null);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatal(final Object message, final Throwable t) {
        log.fatal(FQCN, message, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatal(final String loggerFqcn, final Object message, final Throwable t) {
        log.fatal(loggerFqcn, message, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatal(final String loggerFqcn, final Object message, final Object[] params, final Throwable t) {
        log.fatal(loggerFqcn, message, params, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalv(final String format, final Object... params) {
        log.logv(FQCN, Logger.Level.FATAL, null, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalv(final String format, final Object param1) {
        log.logv(FQCN, Logger.Level.FATAL, null, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalv(final String format, final Object param1, final Object param2) {
        log.logv(FQCN, Logger.Level.FATAL, null, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalv(final String format, final Object param1, final Object param2, final Object param3) {
        log.logv(FQCN, Logger.Level.FATAL, null, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalv(final Throwable t, final String format, final Object... params) {
        log.logv(FQCN, Logger.Level.FATAL, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalv(final Throwable t, final String format, final Object param1) {
        log.logv(FQCN, Logger.Level.FATAL, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalv(final Throwable t, final String format, final Object param1, final Object param2) {
        log.logv(FQCN, Logger.Level.FATAL, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalv(final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logv(FQCN, Logger.Level.FATAL, t, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalf(final String format, final Object... params) {
        log.logf(FQCN, Logger.Level.FATAL, null, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalf(final String format, final Object param1) {
        log.logf(FQCN, Logger.Level.FATAL, null, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalf(final String format, final Object param1, final Object param2) {
        log.logf(FQCN, Logger.Level.FATAL, null, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalf(final String format, final Object param1, final Object param2, final Object param3) {
        log.logf(FQCN, Logger.Level.FATAL, null, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalf(final Throwable t, final String format, final Object... params) {
        log.logf(FQCN, Logger.Level.FATAL, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalf(final Throwable t, final String format, final Object param1) {
        log.logf(FQCN, Logger.Level.FATAL, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalf(final Throwable t, final String format, final Object param1, final Object param2) {
        log.logf(FQCN, Logger.Level.FATAL, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void fatalf(final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logf(FQCN, Logger.Level.FATAL, t, format, param1, param2, param3);
    }


    @Override
    @AlwaysInline("Fast level checks")
    public void log(final Logger.Level level, final Object message) {
        log.log(FQCN, level, message, null, null);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void log(final Logger.Level level, final Object message, final Throwable t) {
        log.log(FQCN, level, message, null, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void log(final Logger.Level level, final String loggerFqcn, final Object message, final Throwable t) {
        log.log(level, loggerFqcn, message, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void log(final String loggerFqcn, final Logger.Level level, final Object message, final Object[] params, final Throwable t) {
        log.log(loggerFqcn, level, message, params, t);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logv(final Logger.Level level, final String format, final Object... params) {
        log.logv(FQCN, level, null, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logv(final Logger.Level level, final String format, final Object param1) {
        log.logv(FQCN, level, null, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logv(final Logger.Level level, final String format, final Object param1, final Object param2) {
        log.logv(FQCN, level, null, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logv(final Logger.Level level, final String format, final Object param1, final Object param2, final Object param3) {
        log.logv(FQCN, level, null, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logv(final Logger.Level level, final Throwable t, final String format, final Object... params) {
        log.logv(FQCN, level, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logv(final Logger.Level level, final Throwable t, final String format, final Object param1) {
        log.logv(FQCN, level, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logv(final Logger.Level level, final Throwable t, final String format, final Object param1, final Object param2) {
        log.logv(FQCN, level, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logv(final Logger.Level level, final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logv(FQCN, level, t, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logv(final String loggerFqcn, final Logger.Level level, final Throwable t, final String format, final Object... params) {
        log.logv(loggerFqcn, level, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logv(final String loggerFqcn, final Logger.Level level, final Throwable t, final String format, final Object param1) {
        log.logv(loggerFqcn, level, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logv(final String loggerFqcn, final Logger.Level level, final Throwable t, final String format, final Object param1, final Object param2) {
        log.logv(loggerFqcn, level, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logv(final String loggerFqcn, final Logger.Level level, final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logv(loggerFqcn, level, t, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logf(final Logger.Level level, final String format, final Object... params) {
        log.logf(FQCN, level, null, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logf(final Logger.Level level, final String format, final Object param1) {
        log.logf(FQCN, level, null, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logf(final Logger.Level level, final String format, final Object param1, final Object param2) {
        log.logf(FQCN, level, null, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logf(final Logger.Level level, final String format, final Object param1, final Object param2, final Object param3) {
        log.logf(FQCN, level, null, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logf(final Logger.Level level, final Throwable t, final String format, final Object... params) {
        log.logf(FQCN, level, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logf(final Logger.Level level, final Throwable t, final String format, final Object param1) {
        log.logf(FQCN, level, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logf(final Logger.Level level, final Throwable t, final String format, final Object param1, final Object param2) {
        log.logf(FQCN, level, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logf(final Logger.Level level, final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logf(FQCN, level, t, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logf(final String loggerFqcn, final Logger.Level level, final Throwable t, final String format, final Object param1) {
        log.logf(loggerFqcn, level, t, format, param1);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logf(final String loggerFqcn, final Logger.Level level, final Throwable t, final String format, final Object param1, final Object param2) {
        log.logf(loggerFqcn, level, t, format, param1, param2);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logf(final String loggerFqcn, final Logger.Level level, final Throwable t, final String format, final Object param1, final Object param2, final Object param3) {
        log.logf(loggerFqcn, level, t, format, param1, param2, param3);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public void logf(final String loggerFqcn, final Logger.Level level, final Throwable t, final String format, final Object... params) {
        log.logf(loggerFqcn, level, t, format, params);
    }

    @Override
    @AlwaysInline("Fast level checks")
    public boolean isEnabled(final Logger.Level level) {
        return log.isEnabled(level);
    }
}
