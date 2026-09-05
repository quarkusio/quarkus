package io.quarkus.produi.runtime.diagnostics;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.json.JsonObject;

/**
 * The single gated, read-only diagnostic action Prod UI exposes: a thread dump.
 * <p>
 * A thread dump reads stack traces via {@link ThreadMXBean} - it mutates nothing, writes no file, and (unlike a heap
 * dump) never captures variable or field values, so it cannot leak secrets. It is nonetheless off by default: the
 * action is only reachable when {@code quarkus.prod-ui.diagnostics.thread-dump=true}, it is still behind whatever
 * secures Prod UI (roles / management auth), the UI asks for confirmation, and every capture is audit-logged here. The
 * provider is only registered when the action is enabled, so this is a defensive re-check.
 */
@ApplicationScoped
public class DiagnosticsProdUIService {

    private static final Logger log = Logger.getLogger(DiagnosticsProdUIService.class);

    @NonBlocking
    @JsonRpcUsage({ Usage.PROD_UI })
    @JsonRpcDescription("Capture a read-only thread dump (stack traces only - no heap dump, no file, no secrets); gated action")
    public JsonObject threadDump() {
        boolean enabled = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.prod-ui.diagnostics.thread-dump", Boolean.class)
                .orElse(Boolean.FALSE);
        if (!enabled) {
            // Defensive: the provider is not registered when disabled, so this should be unreachable.
            log.warn("Prod UI thread dump requested but the action is disabled; refusing.");
            return new JsonObject()
                    .put("enabled", false)
                    .put("message",
                            "Thread dump is disabled. Enable it with quarkus.prod-ui.diagnostics.thread-dump=true.");
        }

        // Audit: this is the only non-read-only-by-default action, so record every capture.
        log.info("Prod UI thread dump captured (read-only diagnostic action).");

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threads = threadMXBean.dumpAllThreads(true, true);
        String dump = ThreadDumpFormatter.format(threads);

        return new JsonObject()
                .put("enabled", true)
                .put("threadCount", threads.length)
                .put("dump", dump);
    }
}
