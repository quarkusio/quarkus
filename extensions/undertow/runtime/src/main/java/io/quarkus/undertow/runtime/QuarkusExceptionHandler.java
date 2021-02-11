package io.quarkus.undertow.runtime;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;

import io.undertow.UndertowLogger;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.ExceptionLog;
import io.undertow.servlet.api.ExceptionHandler;

public class QuarkusExceptionHandler implements ExceptionHandler {

    /**
     * The servlet request attribute that contains the error ID. This can be accessed from a customer error page.
     */
    public static final String ERROR_ID = "quarkus.error.id";

    /**
     * we don't want to generate a new UUID each time as it is slowish. Instead we just generate one based one
     * and then use a counter.
     */
    private static final String BASE_ID = UUID.randomUUID().toString() + "-";

    private static final AtomicLong ERROR_COUNT = new AtomicLong();

    @Override
    public boolean handleThrowable(HttpServerExchange exchange, ServletRequest request, ServletResponse response, Throwable t) {
        String uid = BASE_ID + ERROR_COUNT.incrementAndGet();
        request.setAttribute(ERROR_ID, uid);
        ExceptionLog log = t.getClass().getAnnotation(ExceptionLog.class);
        if (log != null) {
            Logger.Level level = log.value();
            Logger.Level stackTraceLevel = log.stackTraceLevel();
            String category = log.category();
            handleCustomLog(exchange, t, level, stackTraceLevel, category, uid);
        } else if (t instanceof IOException) {
            //we log IOExceptions at a lower level
            //because they can be easily caused by malicious remote clients in at attempt to DOS the server by filling the logs
            UndertowLogger.REQUEST_IO_LOGGER.debugf(t, "Exception handling request %s to %s", uid, exchange.getRequestURI());
        } else {
            UndertowLogger.REQUEST_IO_LOGGER.errorf(t, "Exception handling request %s to %s", uid, exchange.getRequestURI());
        }
        return false;
    }

    private void handleCustomLog(HttpServerExchange exchange, Throwable t, Logger.Level level, Logger.Level stackTraceLevel,
            String category, String uid) {
        BasicLogger logger = UndertowLogger.REQUEST_LOGGER;
        if (!category.isEmpty()) {
            logger = Logger.getLogger(category);
        }
        boolean stackTrace = true;
        if (stackTraceLevel.ordinal() > level.ordinal()) {
            if (!logger.isEnabled(stackTraceLevel)) {
                stackTrace = false;
            }
        }
        if (stackTrace) {
            logger.logf(level, t, "Exception handling request %s to %s", uid, exchange.getRequestURI());
        } else {
            logger.logf(level, "Exception handling request %s to %s: %s", uid, exchange.getRequestURI(), t.getMessage());
        }
    }
}
