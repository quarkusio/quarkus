package io.quarkus.runtime.logging;

import java.util.List;
import java.util.Map;
import java.util.logging.Handler;

/**
 * A holder bundling the extension-contributed logging inputs into a single service value.
 * <p>
 * RECORDER COMPAT: this type exists only to carry legacy build-item contributions (the Dev UI streaming
 * handler, the console banner, the per-named-handler formatters, additional root handlers, and additional named
 * handlers) into the logging configuration service as a single dependency during the migration period. Once the
 * contributing extensions produce services directly, this holder and its consumers can be removed.
 * <p>
 * It is an interface (rather than a record) so that it can be produced by a recorder method and published as a
 * service value via {@code ServiceRegistrar.aliasRecorderValue()}, which requires a proxyable type.
 */
public interface LoggingCompatBridge {

    /**
     * {@return the Dev UI streaming handler, or {@code null} if none}
     */
    StreamingLogHandler streaming();

    /**
     * {@return the console banner, or {@code null} if none}
     */
    LogBanner banner();

    /**
     * {@return the per-named-handler formatters, or {@code null} if none}
     */
    NamedHandlerFormatters namedHandlerFormatters();

    /**
     * {@return the extension-contributed root handlers (never {@code null}, may be empty)}
     */
    List<AdditionalLogHandler> additionalHandlers();

    /**
     * {@return the extension-contributed named handler maps, keyed by handler name (never {@code null}, may be empty)}
     */
    List<Map<String, Handler>> additionalNamedHandlers();

    /**
     * The default implementation of {@link LoggingCompatBridge}.
     *
     * @param streaming the Dev UI streaming handler, or {@code null} if none
     * @param banner the console banner, or {@code null} if none
     * @param namedHandlerFormatters the per-named-handler formatters, or {@code null} if none
     * @param additionalHandlers the extension-contributed root handlers (never {@code null}, may be empty)
     * @param additionalNamedHandlers the extension-contributed named handler maps (never {@code null}, may be empty)
     */
    record Impl(
            StreamingLogHandler streaming,
            LogBanner banner,
            NamedHandlerFormatters namedHandlerFormatters,
            List<AdditionalLogHandler> additionalHandlers,
            List<Map<String, Handler>> additionalNamedHandlers) implements LoggingCompatBridge {
    }
}
