package io.quarkus.devservices.common;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;

/**
 * A TestContainers consumer for container output that logs output to an JBoss logger.
 */
public class JBossLoggingConsumer extends BaseConsumer<JBossLoggingConsumer> {

    private final Logger logger;

    private final Map<String, String> mdc = new HashMap<>();

    private boolean separateOutputStreams;

    private String prefix = "";

    public JBossLoggingConsumer(Logger logger) {
        this(logger, false);
    }

    public JBossLoggingConsumer(Logger logger, boolean separateOutputStreams) {
        this.logger = logger;
        this.separateOutputStreams = separateOutputStreams;
    }

    public JBossLoggingConsumer withPrefix(String prefix) {
        this.prefix = "[" + prefix + "] ";
        return this;
    }

    public JBossLoggingConsumer withMdc(String key, String value) {
        mdc.put(key, value);
        return this;
    }

    public JBossLoggingConsumer withMdc(Map<String, String> mdc) {
        this.mdc.putAll(mdc);
        return this;
    }

    public JBossLoggingConsumer withSeparateOutputStreams() {
        this.separateOutputStreams = true;
        return this;
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        final OutputFrame.OutputType outputType = outputFrame.getType();
        final String utf8String = outputFrame.getUtf8StringWithoutLineEnding();

        final Map<String, Object> originalMdc = MDC.getMap();
        MDC.clear();
        MDC.getMap().putAll(mdc);
        try {
            switch (outputType) {
                case END:
                    break;
                case STDOUT:
                    if (separateOutputStreams) {
                        logger.infof("%s%s", prefix.isEmpty() ? "" : (prefix + ": "), utf8String);
                    } else {
                        logger.infof("%s%s: %s", prefix, outputType, utf8String);
                    }
                    break;
                case STDERR:
                    if (separateOutputStreams) {
                        logger.errorf("%s%s", prefix.isEmpty() ? "" : (prefix + ": "), utf8String);
                    } else {
                        logger.infof("%s%s: %s", prefix, outputType, utf8String);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected outputType " + outputType);
            }
        } finally {
            MDC.clear();
            if (originalMdc != null) {
                MDC.getMap().putAll(originalMdc);
            }
        }
    }
}
