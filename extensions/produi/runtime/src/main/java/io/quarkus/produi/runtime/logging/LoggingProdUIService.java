package io.quarkus.produi.runtime.logging;

import java.util.Enumeration;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;

import io.quarkus.produi.runtime.ProdUISelfFilter;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Read-only Prod UI view of the running application's loggers. For each logger it
 * exposes the name plus its configured and effective log levels, derived from the
 * always-present {@link LogContext}.
 * <p>
 * This is the production-safe counterpart of the Dev UI log viewer. It
 * deliberately mirrors only the read-only {@code getLoggers}/{@code getLogger}
 * behaviour of the Dev UI {@code LogStreamJsonRPCService} - the Dev UI's
 * {@code updateLogLevel} action (which mutates a logger's level at runtime) is
 * intentionally not exposed here, the same way the Cache PoC hides its "clear"
 * action in production. Logger names and levels are not secrets, so nothing
 * sensitive is exposed and nothing is mutated.
 */
@ApplicationScoped
public class LoggingProdUIService {

    @NonBlocking
    @JsonRpcUsage({ Usage.PROD_UI })
    @JsonRpcDescription("Get all the loggers in this Quarkus application with their configured and effective levels")
    public JsonArray getLoggers() {
        boolean excludeSelf = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.prod-ui.exclude-self", Boolean.class).orElse(true);
        LogContext logContext = LogContext.getLogContext();
        JsonArray values = new JsonArray();
        Enumeration<String> loggerNames = logContext.getLoggerNames();
        while (loggerNames.hasMoreElements()) {
            String loggerName = loggerNames.nextElement();
            if (excludeSelf && ProdUISelfFilter.isSelfLogger(loggerName)) {
                continue;
            }
            JsonObject logger = getLogger(loggerName);
            if (logger != null) {
                values.add(logger);
            }
        }
        return values;
    }

    @NonBlocking
    @JsonRpcUsage({ Usage.PROD_UI })
    @JsonRpcDescription("Get a specific logger in this Quarkus application")
    public JsonObject getLogger(@JsonRpcDescription("The name of the logger") String loggerName) {
        if (loggerName == null || loggerName.isEmpty()) {
            return null;
        }
        LogContext logContext = LogContext.getLogContext();
        Logger logger = logContext.getLogger(loggerName);
        return JsonObject.of(
                "name", loggerName.isEmpty() ? "root" : loggerName,
                "effectiveLevel", getEffectiveLogLevel(logger),
                "configuredLevel", getConfiguredLogLevel(logger));
    }

    private String getConfiguredLogLevel(Logger logger) {
        java.util.logging.Level level = logger.getLevel();
        return level != null ? level.getName() : null;
    }

    private String getEffectiveLogLevel(Logger logger) {
        if (logger == null) {
            return null;
        }
        if (logger.getLevel() != null) {
            return logger.getLevel().getName();
        }
        return getEffectiveLogLevel(logger.getParent());
    }
}
