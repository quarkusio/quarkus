/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2014 Red Hat, Inc., and individual contributors
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

import java.util.ResourceBundle;

import java.util.logging.Level;
import java.util.logging.LogRecord;

class WrappedExtLogRecord extends ExtLogRecord {

    private static final long serialVersionUID = 980830752574061944L;
    private static final String LOGGER_CLASS_NAME = java.util.logging.Logger.class.getName();

    private transient final LogRecord orig;
    private transient boolean resolved;

    WrappedExtLogRecord(final LogRecord orig) {
        super(orig.getLevel(), orig.getMessage(), LOGGER_CLASS_NAME);
        this.orig = orig;
    }

    public String getLoggerName() {
        return orig.getLoggerName();
    }

    public void setLoggerName(final String name) {
        super.setLoggerName(name);
        orig.setLoggerName(name);
    }

    public ResourceBundle getResourceBundle() {
        return orig.getResourceBundle();
    }

    public void setResourceBundle(final ResourceBundle bundle) {
        super.setResourceBundle(bundle);
        orig.setResourceBundle(bundle);
    }

    public String getResourceBundleName() {
        return orig.getResourceBundleName();
    }

    public void setResourceBundleName(final String name) {
        super.setResourceBundleName(name);
        orig.setResourceBundleName(name);
    }

    public Level getLevel() {
        return orig.getLevel();
    }

    public void setLevel(final Level level) {
        super.setLevel(level);
        orig.setLevel(level);
    }

    public long getSequenceNumber() {
        return orig.getSequenceNumber();
    }

    public void setSequenceNumber(final long seq) {
        super.setSequenceNumber(seq);
        orig.setSequenceNumber(seq);
    }

    public String getSourceClassName() {
        if (! resolved) {
            resolve();
        }
        return super.getSourceClassName();
    }

    public void setSourceClassName(final String sourceClassName) {
        resolved = true;
        super.setSourceClassName(sourceClassName);
        orig.setSourceClassName(sourceClassName);
    }

    public String getSourceMethodName() {
        if (! resolved) {
            resolve();
        }
        return super.getSourceMethodName();
    }

    public void setSourceMethodName(final String sourceMethodName) {
        resolved = true;
        super.setSourceMethodName(sourceMethodName);
        orig.setSourceMethodName(sourceMethodName);
    }

    private void resolve() {
        resolved = true;
        final String sourceMethodName = orig.getSourceMethodName();
        final String sourceClassName = orig.getSourceClassName();
        super.setSourceMethodName(sourceMethodName);
        super.setSourceClassName(sourceClassName);
        final StackTraceElement[] st = new Throwable().getStackTrace();
        for (StackTraceElement element : st) {
            if (element.getClassName().equals(sourceClassName) && element.getMethodName().equals(sourceMethodName)) {
                super.setSourceLineNumber(element.getLineNumber());
                super.setSourceFileName(element.getFileName());
                return;
            }
        }
    }

    public int getSourceLineNumber() {
        if (! resolved) {
            resolve();
        }
        return super.getSourceLineNumber();
    }

    public void setSourceLineNumber(final int sourceLineNumber) {
        resolved = true;
        super.setSourceLineNumber(sourceLineNumber);
    }

    public String getSourceFileName() {
        if (! resolved) {
            resolve();
        }
        return super.getSourceFileName();
    }

    public void setSourceFileName(final String sourceFileName) {
        resolved = true;
        super.setSourceFileName(sourceFileName);
    }

    public String getMessage() {
        return orig.getMessage();
    }

    public void setMessage(final String message) {
        super.setMessage(message);
        orig.setMessage(message);
    }

    public Object[] getParameters() {
        return orig.getParameters();
    }

    public void setParameters(final Object[] parameters) {
        orig.setParameters(parameters);
    }

    public int getThreadID() {
        return orig.getThreadID();
    }

    public void setThreadID(final int threadID) {
        orig.setThreadID(threadID);
    }

    public long getMillis() {
        return orig.getMillis();
    }

    public void setMillis(final long millis) {
        orig.setMillis(millis);
    }

    public Throwable getThrown() {
        return orig.getThrown();
    }

    public void setThrown(final Throwable thrown) {
        orig.setThrown(thrown);
    }

    protected Object writeReplace() {
        return new ExtLogRecord(this);
    }
}
