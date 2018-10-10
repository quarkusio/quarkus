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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.MessageFormat;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import java.util.logging.LogRecord;

import org.wildfly.common.net.HostName;
import org.wildfly.common.os.Process;

/**
 * An extended log record, which includes additional information including MDC/NDC and correct
 * caller location (even in the presence of a logging facade).
 */
public class ExtLogRecord extends LogRecord {

    private static final long serialVersionUID = -9174374711278052369L;

    /**
     * The format style to use.
     */
    public enum FormatStyle {

        /**
         * Format the message using the {@link java.text.MessageFormat} parameter style.
         */
        MESSAGE_FORMAT,
        /**
         * Format the message using the {@link java.util.Formatter} (also known as {@code printf()}) parameter style.
         */
        PRINTF,
        /**
         * Do not format the message; parameters are ignored.
         */
        NO_FORMAT,
    }

    /**
     * Construct a new instance.  Grabs the current NDC immediately.  MDC is deferred.
     *
     * @param level a logging level value
     * @param msg the raw non-localized logging message (may be null)
     * @param loggerClassName the name of the logger class
     */
    public ExtLogRecord(final java.util.logging.Level level, final String msg, final String loggerClassName) {
        this(level, msg, FormatStyle.MESSAGE_FORMAT, loggerClassName);
    }

    /**
     * Construct a new instance.  Grabs the current NDC immediately.  MDC is deferred.
     *
     * @param level a logging level value
     * @param msg the raw non-localized logging message (may be null)
     * @param formatStyle the parameter format style to use
     * @param loggerClassName the name of the logger class
     */
    public ExtLogRecord(final java.util.logging.Level level, final String msg, final FormatStyle formatStyle, final String loggerClassName) {
        super(level, msg);
        this.formatStyle = formatStyle == null ? FormatStyle.MESSAGE_FORMAT : formatStyle;
        this.loggerClassName = loggerClassName;
        ndc = NDC.get();
        threadName = Thread.currentThread().getName();
        hostName = HostName.getQualifiedHostName();
        processName = Process.getProcessName();
        processId = Process.getProcessId();
    }

    /**
     * Make a copy of a log record.
     *
     * @param original the original
     */
    public ExtLogRecord(final ExtLogRecord original) {
        super(original.getLevel(), original.getMessage());
        // LogRecord fields
        setLoggerName(original.getLoggerName());
        setMillis(original.getMillis());
        setParameters(original.getParameters());
        setResourceBundle(original.getResourceBundle());
        setResourceBundleName(original.getResourceBundleName());
        setSequenceNumber(original.getSequenceNumber());
        setThreadID(original.getThreadID());
        setThrown(original.getThrown());
        if (!original.calculateCaller) {
            setSourceClassName(original.getSourceClassName());
            setSourceMethodName(original.getSourceMethodName());
            sourceFileName = original.sourceFileName;
            sourceLineNumber = original.sourceLineNumber;
            sourceModuleName = original.sourceModuleName;
            sourceModuleVersion = original.sourceModuleVersion;
        }
        formatStyle = original.formatStyle;
        mdcCopy = original.mdcCopy;
        ndc = original.ndc;
        loggerClassName = original.loggerClassName;
        threadName = original.threadName;
        hostName = original.hostName;
        processName = original.processName;
        processId = original.processId;
    }

    /**
     * Wrap a JDK log record.  If the target record is already an {@code ExtLogRecord}, it is simply returned.  Otherwise
     * a wrapper record is created and returned.
     *
     * @param rec the original record
     * @return the wrapped record
     */
    public static ExtLogRecord wrap(LogRecord rec) {
        if (rec == null) {
            return null;
        } else if (rec instanceof ExtLogRecord) {
            return (ExtLogRecord) rec;
        } else {
            return new WrappedExtLogRecord(rec);
        }
    }

    private final transient String loggerClassName;
    private transient boolean calculateCaller = true;

    private String ndc;
    private FormatStyle formatStyle = FormatStyle.MESSAGE_FORMAT;
    private FastCopyHashMap<String, Object> mdcCopy;
    private int sourceLineNumber = -1;
    private String sourceFileName;
    private String threadName;
    private String hostName;
    private String processName;
    private long processId = -1;
    private String sourceModuleName;
    private String sourceModuleVersion;

    private void writeObject(ObjectOutputStream oos) throws IOException {
        copyAll();
        oos.defaultWriteObject();
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        final ObjectInputStream.GetField fields = ois.readFields();
        ndc = (String) fields.get("ndc", null);
        formatStyle = (FormatStyle) fields.get("formatStyle", FormatStyle.MESSAGE_FORMAT);
        mdcCopy = (FastCopyHashMap<String, Object>) fields.get("mdcCopy", new FastCopyHashMap<>());
        sourceLineNumber = fields.get("sourceLineNumber", -1);
        sourceFileName = (String) fields.get("sourceFileName", null);
        threadName = (String) fields.get("threadName", null);
        hostName = (String) fields.get("hostName", null);
        processName = (String) fields.get("processName", null);
        processId = fields.get("processId", -1L);
        sourceModuleName = (String) fields.get("sourceModuleName", null);
        sourceModuleVersion = (String) fields.get("sourceModuleVersion", null);
    }

    /**
     * Disable caller calculation for this record.  If the caller has already been calculated, leave it; otherwise
     * set the caller to {@code "unknown"}.
     */
    public void disableCallerCalculation() {
        if (calculateCaller) {
            setUnknownCaller();
        }
    }

    /**
     * Copy all fields and prepare this object to be passed to another thread or to be serialized.  Calling this method
     * more than once has no additional effect and will not incur extra copies.
     */
    public void copyAll() {
        copyMdc();
        calculateCaller();
    }

    /**
     * Copy the MDC.  Call this method before passing this log record to another thread.  Calling this method
     * more than once has no additional effect and will not incur extra copies.
     */
    public void copyMdc() {
        if (mdcCopy == null) {
            mdcCopy = MDC.fastCopyObject();
        }
    }

    /**
     * Get the value of an MDC property.
     *
     * @param key the property key
     * @return the property value
     */
    public String getMdc(String key) {
        final Map<String, Object> mdcCopy = this.mdcCopy;
        if (mdcCopy == null) {
            return MDC.get(key);
        }
        final Object value =  mdcCopy.get(key);
        return value == null ? null : value.toString();
    }

    /**
     * Get a copy of all the MDC properties for this log record.  If the MDC has not yet been copied, this method will copy it.
     *
     * @return a copy of the MDC map
     */
    public Map<String, String> getMdcCopy() {
        if (mdcCopy == null) {
            mdcCopy = MDC.fastCopyObject();
        }
        // Create a new map with string values
        final FastCopyHashMap<String, String> newMdc = new FastCopyHashMap<String, String>();
        for (Map.Entry<String, Object> entry : mdcCopy.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            newMdc.put(key, (value == null ? null : value.toString()));
        }
        return newMdc;
    }

    /**
     * Change an MDC value on this record.  If the MDC has not yet been copied, this method will copy it.
     *
     * @param key the key to set
     * @param value the value to set it to
     * @return the old value, if any
     */
    public String putMdc(String key, String value) {
        copyMdc();
        final Object oldValue = mdcCopy.put(key, value);
        return oldValue == null ? null : oldValue.toString();
    }

    /**
     * Remove an MDC value on this record.  If the MDC has not yet been copied, this method will copy it.
     *
     * @param key the key to remove
     * @return the old value, if any
     */
    public String removeMdc(String key) {
        copyMdc();
        final Object oldValue = mdcCopy.remove(key);
        return oldValue == null ? null : oldValue.toString();
    }

    /**
     * Create a new MDC using a copy of the source map.
     *
     * @param sourceMap the source man, must not be {@code null}
     */
    public void setMdc(Map<?, ?> sourceMap) {
        final FastCopyHashMap<String, Object> newMdc = new FastCopyHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
            final Object key = entry.getKey();
            final Object value = entry.getValue();
            if (key != null && value != null) {
                newMdc.put(key.toString(), value);
            }
        }
        mdcCopy = newMdc;
    }

    /**
     * Get the NDC for this log record.
     *
     * @return the NDC
     */
    public String getNdc() {
        return ndc;
    }

    /**
     * Change the NDC for this log record.
     *
     * @param value the new NDC value
     */
    public void setNdc(String value) {
        ndc = value;
    }

    /**
     * Get the class name of the logger which created this record.
     *
     * @return the class name
     */
    public String getLoggerClassName() {
        return loggerClassName;
    }

    /**
     * Get the format style for the record.
     *
     * @return the format style
     */
    public FormatStyle getFormatStyle() {
        return formatStyle;
    }

    /**
     * Find the first stack frame below the call to the logger, and populate the log record with that information.
     */
    private void calculateCaller() {
        if (! calculateCaller) {
            return;
        }
        calculateCaller = false;
        JDKSpecific.calculateCaller(this);
    }

    void setUnknownCaller() {
        setSourceClassName(null);
        setSourceMethodName(null);
        setSourceLineNumber(-1);
        setSourceFileName(null);
        setSourceModuleName(null);
        setSourceModuleVersion(null);
    }

    /**
     * Get the source line number for this log record.
     * <p/>
     * Note that this line number is not verified and may be spoofed. This information may either have been
     * provided as part of the logging call, or it may have been inferred automatically by the logging framework. In the
     * latter case, the information may only be approximate and may in fact describe an earlier call on the stack frame.
     * May be -1 if no information could be obtained.
     *
     * @return the source line number
     */
    public int getSourceLineNumber() {
        calculateCaller();
        return sourceLineNumber;
    }

    /**
     * Set the source line number for this log record.
     *
     * @param sourceLineNumber the source line number
     */
    public void setSourceLineNumber(final int sourceLineNumber) {
        calculateCaller = false;
        this.sourceLineNumber = sourceLineNumber;
    }

    /**
     * Get the source file name for this log record.
     * <p/>
     * Note that this file name is not verified and may be spoofed. This information may either have been
     * provided as part of the logging call, or it may have been inferred automatically by the logging framework. In the
     * latter case, the information may only be approximate and may in fact describe an earlier call on the stack frame.
     * May be {@code null} if no information could be obtained.
     *
     * @return the source file name
     */
    public String getSourceFileName() {
        calculateCaller();
        return sourceFileName;
    }

    /**
     * Set the source file name for this log record.
     *
     * @param sourceFileName the source file name
     */
    public void setSourceFileName(final String sourceFileName) {
        calculateCaller = false;
        this.sourceFileName = sourceFileName;
    }

    /** {@inheritDoc} */
    public String getSourceClassName() {
        calculateCaller();
        return super.getSourceClassName();
    }

    /** {@inheritDoc} */
    public void setSourceClassName(final String sourceClassName) {
        calculateCaller = false;
        super.setSourceClassName(sourceClassName);
    }

    /** {@inheritDoc} */
    public String getSourceMethodName() {
        calculateCaller();
        return super.getSourceMethodName();
    }

    /** {@inheritDoc} */
    public void setSourceMethodName(final String sourceMethodName) {
        calculateCaller = false;
        super.setSourceMethodName(sourceMethodName);
    }

    /**
     * Get the name of the module that initiated the logging request, if known.
     *
     * @return the name of the module that initiated the logging request
     */
    public String getSourceModuleName() {
        calculateCaller();
        return sourceModuleName;
    }

    /**
     * Set the source module name of this record.
     *
     * @param sourceModuleName the source module name
     */
    public void setSourceModuleName(final String sourceModuleName) {
        calculateCaller = false;
        this.sourceModuleName = sourceModuleName;
    }

    /**
     * Get the version of the module that initiated the logging request, if known.
     *
     * @return the version of the module that initiated the logging request
     */
    public String getSourceModuleVersion() {
        calculateCaller();
        return sourceModuleVersion;
    }

    /**
     * Set the source module version of this record.
     *
     * @param sourceModuleVersion the source module version
     */
    public void setSourceModuleVersion(final String sourceModuleVersion) {
        calculateCaller = false;
        this.sourceModuleVersion = sourceModuleVersion;
    }

    /**
     * Get the fully formatted log record, with resources resolved and parameters applied.
     *
     * @return the formatted log record
     * @deprecated The formatter should normally be used to format the message contents.
     */
    @Deprecated
    public String getFormattedMessage() {
        final ResourceBundle bundle = getResourceBundle();
        String msg = getMessage();
        if (msg == null)
            return null;
        if (bundle != null) {
            try {
                msg = bundle.getString(msg);
            } catch (MissingResourceException ex) {
                // ignore
            }
        }
        final Object[] parameters = getParameters();
        if (parameters == null || parameters.length == 0) {
            return msg;
        }
        switch (formatStyle) {
            case PRINTF: {
                return String.format(msg, parameters);
            }
            case MESSAGE_FORMAT: {
                return msg.indexOf('{') >= 0 ? MessageFormat.format(msg, parameters) : msg;
            }
        }
        // should be unreachable
        return msg;
    }

    /**
     * Get the resource key, if any.  If the log message is not localized, then the key is {@code null}.
     *
     * @return the resource key
     */
    public String getResourceKey() {
        final String msg = getMessage();
        if (msg == null) return null;
        if (getResourceBundleName() == null && getResourceBundle() == null) return null;
        return msg;
    }

    /**
     * Get the thread name of this logging event.
     *
     * @return the thread name
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * Set the thread name of this logging event.
     *
     * @param threadName the thread name
     */
    public void setThreadName(final String threadName) {
        this.threadName = threadName;
    }

    /**
     * Get the host name of the record, if known.
     *
     * @return the host name of the record, if known
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Set the host name of the record.
     *
     * @param hostName the host name of the record
     */
    public void setHostName(final String hostName) {
        this.hostName = hostName;
    }

    /**
     * Get the process name of the record, if known.
     *
     * @return the process name of the record, if known
     */
    public String getProcessName() {
        return processName;
    }

    /**
     * Set the process name of the record.
     *
     * @param processName the process name of the record
     */
    public void setProcessName(final String processName) {
        this.processName = processName;
    }

    /**
     * Get the process ID of the record, if known.
     *
     * @return the process ID of the record, or -1 if not known
     */
    public long getProcessId() {
        return processId;
    }

    /**
     * Set the process ID of the record.
     *
     * @param processId the process ID of the record
     */
    public void setProcessId(final long processId) {
        this.processId = processId;
    }

    /**
     * Set the raw message.  Any cached formatted message is discarded.  The parameter format is set to be
     * {@link java.text.MessageFormat}-style.
     *
     * @param message the new raw message
     */
    public void setMessage(final String message) {
        setMessage(message, FormatStyle.MESSAGE_FORMAT);
    }

    /**
     * Set the raw message.  Any cached formatted message is discarded.  The parameter format is set according to the
     * given argument.
     *
     * @param message the new raw message
     * @param formatStyle the format style to use
     */
    public void setMessage(final String message, final FormatStyle formatStyle) {
        this.formatStyle = formatStyle == null ? FormatStyle.MESSAGE_FORMAT : formatStyle;
        super.setMessage(message);
    }

    /**
     * Set the parameters to the log message.  Any cached formatted message is discarded.
     *
     * @param parameters the log message parameters. (may be null)
     */
    public void setParameters(final Object[] parameters) {
        super.setParameters(parameters);
    }

    /**
     * Set the localization resource bundle.  Any cached formatted message is discarded.
     *
     * @param bundle localization bundle (may be null)
     */
    public void setResourceBundle(final ResourceBundle bundle) {
        super.setResourceBundle(bundle);
    }

    /**
     * Set the localization resource bundle name.  Any cached formatted message is discarded.
     *
     * @param name localization bundle name (may be null)
     */
    public void setResourceBundleName(final String name) {
        super.setResourceBundleName(name);
    }
}
