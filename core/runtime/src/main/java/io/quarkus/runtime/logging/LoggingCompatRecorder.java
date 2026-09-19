package io.quarkus.runtime.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.Handler;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Transitional recorder that unwraps legacy logging build-item values (which are delivered as
 * {@link RuntimeValue}-wrapped optionals and maps) so that they can be published into the logging service
 * graph via {@code ActionBuilder.aliasRecorderValue()}.
 * <p>
 * RECORDER COMPAT: this entire recorder exists only to bridge extension-contributed logging build items into
 * the service scheme during the migration period. Each method here can be removed once the corresponding
 * contributing extensions produce services directly.
 */
@Recorder
public class LoggingCompatRecorder {

    /**
     * Resolve the effective formatter from a list of extension-contributed candidate formatters, using the
     * last present one (matching the legacy behavior). Returns {@code null} if none is present.
     *
     * @param possibleFormatters the candidate formatters (must not be {@code null})
     * @return the resolved formatter, or {@code null} if none is present
     */
    public Formatter resolveFormatter(List<RuntimeValue<Optional<Formatter>>> possibleFormatters) {
        Formatter formatter = null;
        for (RuntimeValue<Optional<Formatter>> value : possibleFormatters) {
            Optional<Formatter> val = value.getValue();
            if (val.isPresent()) {
                formatter = val.get();
            }
        }
        return formatter;
    }

    /**
     * Bundle the extension-contributed logging inputs into a single {@link LoggingCompatBridge} holder,
     * unwrapping the {@link RuntimeValue}-wrapped values.
     *
     * @param streamingHandler the Dev UI streaming handler value, or {@code null} if none
     * @param banner the console banner supplier value, or {@code null} if none
     * @param namedHandlerFormatters the contributed named-handler formatter maps (must not be {@code null})
     * @param additionalHandlers the contributed root handler values (must not be {@code null})
     * @return the bundled bridge holder (never {@code null})
     */
    public LoggingCompatBridge buildBridge(
            RuntimeValue<Optional<Handler>> streamingHandler,
            RuntimeValue<Optional<Supplier<String>>> banner,
            List<RuntimeValue<Map<NamedHandlerType, Map<String, Optional<Formatter>>>>> namedHandlerFormatters,
            List<RuntimeValue<Optional<Handler>>> additionalHandlers) {
        StreamingLogHandler streaming = streamingHandler == null ? null
                : streamingHandler.getValue().map(StreamingLogHandler::new).orElse(null);
        LogBanner logBanner = banner == null ? null
                : banner.getValue().map(LogBanner::new).orElse(null);
        NamedHandlerFormatters formatters = namedHandlerFormatters.isEmpty() ? null
                : new NamedHandlerFormatters(LoggingSetup.mergeNamedHandlerFormatters(namedHandlerFormatters));
        List<AdditionalLogHandler> additional = new ArrayList<>(additionalHandlers.size());
        for (RuntimeValue<Optional<Handler>> handler : additionalHandlers) {
            handler.getValue().ifPresent(h -> additional.add(new AdditionalLogHandler(h)));
        }
        return new LoggingCompatBridge.Impl(streaming, logBanner, formatters, List.copyOf(additional));
    }
}
