package io.quarkus.devui.runtime.logstream;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Formatting log records into a json format
 */
public class JsonFormatter extends ExtFormatter {

    @Override
    public String format(final ExtLogRecord logRecord) {
        return toJsonObject(logRecord).encodePrettily();
    }

    JsonObject toJsonObject(ExtLogRecord logRecord) {
        String formattedMessage = formatMessage(logRecord);

        JsonObject jsonObject = JsonObject.of();

        jsonObject.put(TYPE, LOG_LINE);
        if (logRecord.getLoggerName() != null) {
            jsonObject.put(LOGGER_NAME_SHORT, getShortFullClassName(logRecord.getLoggerName(), ""));
            jsonObject.put(LOGGER_NAME, logRecord.getLoggerName());
        }
        if (logRecord.getLoggerClassName() != null) {
            jsonObject.put(LOGGER_CLASS_NAME, logRecord.getLoggerClassName());
        }
        if (logRecord.getHostName() != null) {
            jsonObject.put(HOST_NAME, logRecord.getHostName());
        }
        if (logRecord.getLevel() != null) {
            jsonObject.put(LEVEL, logRecord.getLevel().getName());
        }
        if (formattedMessage != null) {
            jsonObject.put(FORMATTED_MESSAGE, formattedMessage);
        }
        if (logRecord.getMessage() != null) {
            jsonObject.put(MESSAGE, logRecord.getMessage());
        }
        jsonObject.put(SOURCE_LINE_NUMBER, logRecord.getSourceLineNumber());
        if (logRecord.getSourceClassName() != null) {
            String justClassName = getJustClassName(logRecord.getSourceClassName());
            jsonObject.put(SOURCE_CLASS_NAME_FULL_SHORT, getShortFullClassName(logRecord.getSourceClassName(), justClassName));
            jsonObject.put(SOURCE_CLASS_NAME_FULL, logRecord.getSourceClassName());
            jsonObject.put(SOURCE_CLASS_NAME, justClassName);
        }
        if (logRecord.getSourceFileName() != null) {
            jsonObject.put(SOURCE_FILE_NAME, logRecord.getSourceFileName());
        }
        if (logRecord.getSourceMethodName() != null) {
            jsonObject.put(SOURCE_METHOD_NAME, logRecord.getSourceMethodName());
        }
        if (logRecord.getThrown() != null) {
            jsonObject.put(STACKTRACE, getStacktraces(logRecord.getThrown()));
        }
        jsonObject.put(THREAD_ID, logRecord.getThreadID());
        jsonObject.put(THREAD_NAME, logRecord.getThreadName());
        jsonObject.put(PROCESS_ID, logRecord.getProcessId());
        jsonObject.put(PROCESS_NAME, logRecord.getProcessName());

        jsonObject.put(TIMESTAMP, getTimeStampInCurrentZone(logRecord.getMillis()));
        jsonObject.put(SEQUENCE_NUMBER, logRecord.getSequenceNumber());
        return jsonObject;
    }

    private String getTimeStampInCurrentZone(long epochMilli) {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        ZonedDateTime dateTime = instant.atZone(ZoneId.systemDefault());
        return dateTime.format(formatter);
    }

    private JsonArray getStacktraces(Throwable t) {
        List<String> traces = new LinkedList<>();
        addStacktrace(traces, t);

        JsonArray jsonArray = JsonArray.of();

        traces.forEach((trace) -> {
            jsonArray.add(trace);
        });
        return jsonArray;
    }

    private void addStacktrace(List<String> traces, Throwable t) {
        traces.add(getStacktrace(t));
        if (t.getCause() != null)
            addStacktrace(traces, t.getCause());
    }

    private String getStacktrace(Throwable t) {
        try (StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
            return sw.toString();
        } catch (IOException ex) {
            return null;
        }
    }

    private String getJustClassName(String fullName) {
        int lastDot = fullName.lastIndexOf(DOT) + 1;
        return fullName.substring(lastDot);
    }

    private String getShortFullClassName(String fullName, String justClassName) {
        String[] parts = fullName.split("\\" + DOT);
        try (StringWriter buffer = new StringWriter()) {
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                if (part.equals(justClassName) || part.length() < 3) {
                    buffer.write(part);
                } else {
                    buffer.write(part.substring(0, 3));
                }
                buffer.write(DOT);
            }
            buffer.write(parts[parts.length - 1]);
            return buffer.toString();
        } catch (IOException ex) {
            return fullName;
        }
    }

    private static final String TYPE = "type";
    private static final String LEVEL = "level";
    private static final String MESSAGE = "message";
    private static final String FORMATTED_MESSAGE = "formattedMessage";
    private static final String LOGGER_NAME_SHORT = "loggerNameShort";
    private static final String LOGGER_NAME = "loggerName";
    private static final String LOGGER_CLASS_NAME = "loggerClassName";
    private static final String HOST_NAME = "hostName";
    private static final String SOURCE_LINE_NUMBER = "sourceLineNumber";
    private static final String SOURCE_CLASS_NAME_FULL = "sourceClassNameFull";
    private static final String SOURCE_CLASS_NAME_FULL_SHORT = "sourceClassNameFullShort";
    private static final String SOURCE_CLASS_NAME = "sourceClassName";
    private static final String SOURCE_FILE_NAME = "sourceFileName";
    private static final String SOURCE_METHOD_NAME = "sourceMethodName";
    private static final String THREAD_ID = "threadId";
    private static final String THREAD_NAME = "threadName";
    private static final String PROCESS_ID = "processId";
    private static final String PROCESS_NAME = "processName";
    private static final String TIMESTAMP = "timestamp";
    private static final String STACKTRACE = "stacktrace";
    private static final String SEQUENCE_NUMBER = "sequenceNumber";
    private static final String DOT = ".";
    private static final String LOG_LINE = "logLine";

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
}
