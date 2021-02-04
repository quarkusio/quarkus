package io.quarkus.vertx.http.runtime.logstream;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
        return toJsonObject(logRecord).toString();
    }

    private JsonObject toJsonObject(ExtLogRecord logRecord) {
        String formattedMessage = formatMessage(logRecord);
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(TYPE, LOG_LINE);
        if (logRecord.getLoggerName() != null) {
            jsonObject.put(LOGGER_NAME, logRecord.getLoggerName());
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
        if (logRecord.getSourceClassName() != null) {
            String justClassName = getJustClassName(logRecord.getSourceClassName());
            jsonObject.put(SOURCE_CLASS_NAME_FULL_SHORT, getShortFullClassName(logRecord.getSourceClassName(), justClassName));
            jsonObject.put(SOURCE_CLASS_NAME_FULL, logRecord.getSourceClassName());
            jsonObject.put(SOURCE_CLASS_NAME, justClassName);
        }
        if (logRecord.getSourceMethodName() != null) {
            jsonObject.put(SOURCE_METHOD_NAME, logRecord.getSourceMethodName());
        }
        if (logRecord.getThrown() != null) {
            jsonObject.put(STACKTRACE, getStacktraces(logRecord.getThrown()));
        }
        jsonObject.put(THREAD_ID, logRecord.getThreadID());
        jsonObject.put(THREAD_NAME, Thread.currentThread().getName());
        jsonObject.put(TIMESTAMP, logRecord.getMillis());
        jsonObject.put(SEQUENCE_NUMBER, logRecord.getSequenceNumber());

        return jsonObject;
    }

    private JsonArray getStacktraces(Throwable t) {
        List<String> traces = new LinkedList<>();
        addStacktrace(traces, t);

        JsonArray jsonArray = new JsonArray();

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
            for (String part : parts) {
                if (part.equals(justClassName) || part.length() < 3) {
                    buffer.write(part);
                } else {
                    buffer.write(part.substring(0, 3));
                }
                buffer.write(DOT);
            }
            String r = buffer.toString();

            return r.substring(0, r.lastIndexOf(DOT));
        } catch (IOException ex) {
            return fullName;
        }
    }

    private static final String TYPE = "type";
    private static final String LEVEL = "level";
    private static final String MESSAGE = "message";
    private static final String FORMATTED_MESSAGE = "formattedMessage";
    private static final String LOGGER_NAME = "loggerName";
    private static final String SOURCE_CLASS_NAME_FULL = "sourceClassNameFull";
    private static final String SOURCE_CLASS_NAME_FULL_SHORT = "sourceClassNameFullShort";
    private static final String SOURCE_CLASS_NAME = "sourceClassName";
    private static final String SOURCE_METHOD_NAME = "sourceMethodName";
    private static final String THREAD_ID = "threadId";
    private static final String THREAD_NAME = "threadName";
    private static final String TIMESTAMP = "timestamp";
    private static final String STACKTRACE = "stacktrace";
    private static final String SEQUENCE_NUMBER = "sequenceNumber";
    private static final String DOT = ".";
    private static final String LOG_LINE = "logLine";
}
