package io.quarkus.deployment.dev.testing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.CuratedApplication;

public class LogCapturingOutputFilter implements BiPredicate<String, Boolean> {
    private static final Logger log = Logger.getLogger(LogCapturingOutputFilter.class);

    private final CuratedApplication application;
    private final List<String> logOutput = new ArrayList<>();
    private final List<String> errorOutput = new ArrayList<>();
    private final boolean mergeErrorStream;
    private final boolean convertToHtml;
    private final Supplier<Boolean> finalPredicate;

    public LogCapturingOutputFilter(CuratedApplication application, boolean mergeErrorStream, boolean convertToHtml,
            Supplier<Boolean> finalPredicate) {
        this.application = application;
        this.mergeErrorStream = mergeErrorStream;
        this.convertToHtml = convertToHtml;
        this.finalPredicate = finalPredicate;
    }

    public List<String> captureOutput() {
        List<String> ret = new ArrayList<>(logOutput);
        logOutput.clear();
        return ret;
    }

    public List<String> captureErrorOutput() {
        List<String> ret = new ArrayList<>(errorOutput);
        errorOutput.clear();
        return ret;
    }

    @Override
    public boolean test(String logRecord, Boolean errorStream) {
        Thread thread = Thread.currentThread();
        ClassLoader cl = thread.getContextClassLoader();
        if (cl == null) {
            return true;
        }
        while (cl.getParent() != null) {
            if (cl == application.getAugmentClassLoader()
                    || cl == application.getBaseRuntimeClassLoader()) {
                //TODO: for convenience we save the log records as HTML rather than ANSI here
                synchronized (logOutput) {
                    if (convertToHtml) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        HtmlAnsiOutputStream outputStream = new HtmlAnsiOutputStream(out) {
                        };
                        try {
                            outputStream.write(logRecord.getBytes(StandardCharsets.UTF_8));
                            if (mergeErrorStream || !errorStream) {
                                logOutput.add(out.toString(StandardCharsets.UTF_8));
                            } else {
                                errorOutput.add(out.toString(StandardCharsets.UTF_8));
                            }
                        } catch (IOException e) {
                            log.error("Failed to capture log record", e);
                            logOutput.add(logRecord);
                        }
                    } else {
                        if (mergeErrorStream || !errorStream) {
                            logOutput.add(logRecord);
                        } else {
                            errorOutput.add(logRecord);
                        }
                    }
                }
                return finalPredicate.get();
            }
            cl = cl.getParent();
        }
        return true;
    }
}
