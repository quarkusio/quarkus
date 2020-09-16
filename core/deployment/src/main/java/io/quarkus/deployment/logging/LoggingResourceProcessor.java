package io.quarkus.deployment.logging;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.stream.Collectors;

import org.jboss.logmanager.EmbeddedConfigurator;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConsoleFormatterBannerBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.LogConsoleFormatBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.builditem.NamedLogHandlersBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.logging.LogBuildTimeConfig;
import io.quarkus.runtime.logging.LogConfig;
import io.quarkus.runtime.logging.LogMetricsHandlerRecorder;
import io.quarkus.runtime.logging.LoggingSetupRecorder;

public final class LoggingResourceProcessor {

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        filters.produce(new LogCleanupFilterBuildItem("org.jboss.threads", "JBoss Threads version"));
    }

    @BuildStep
    SystemPropertyBuildItem setProperty() {
        return new SystemPropertyBuildItem("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    @BuildStep
    void setUpDefaultLevels(List<LogCategoryBuildItem> categories,
            Consumer<RunTimeConfigurationDefaultBuildItem> configOutput) {
        for (LogCategoryBuildItem category : categories) {
            configOutput.accept(
                    new RunTimeConfigurationDefaultBuildItem(
                            "quarkus.log.category.\"" + category.getCategory() + "\".level",
                            category.getLevel().toString()));
        }
    }

    @BuildStep
    void setUpDefaultLogCleanupFilters(List<LogCleanupFilterBuildItem> logCleanupFilters,
            Consumer<RunTimeConfigurationDefaultBuildItem> configOutput) {
        for (LogCleanupFilterBuildItem logCleanupFilter : logCleanupFilters) {
            String startsWithClause = logCleanupFilter.getFilterElement().getMessageStarts().stream()
                    // SmallRye Config escaping is pretty naive so we only need to escape commas
                    .map(s -> s.replace(",", "\\,"))
                    .collect(Collectors.joining(","));
            configOutput.accept(
                    new RunTimeConfigurationDefaultBuildItem(
                            "quarkus.log.filter.\"" + logCleanupFilter.getFilterElement().getLoggerName()
                                    + "\".if-starts-with",
                            startsWithClause));
            if (logCleanupFilter.getFilterElement().getTargetLevel() != null) {
                configOutput.accept(
                        new RunTimeConfigurationDefaultBuildItem(
                                "quarkus.log.filter.\"" + logCleanupFilter.getFilterElement().getLoggerName()
                                        + "\".target-level",
                                logCleanupFilter.getFilterElement().getTargetLevel().toString()));
            }
        }
    }

    @BuildStep
    void miscSetup(
            Consumer<RuntimeInitializedClassBuildItem> runtimeInit,
            Consumer<NativeImageSystemPropertyBuildItem> systemProp,
            Consumer<ServiceProviderBuildItem> provider) {
        runtimeInit.accept(new RuntimeInitializedClassBuildItem("org.jboss.logmanager.formatters.TrueColorHolder"));
        systemProp
                .accept(new NativeImageSystemPropertyBuildItem("java.util.logging.manager", "org.jboss.logmanager.LogManager"));
        provider.accept(
                new ServiceProviderBuildItem(EmbeddedConfigurator.class.getName(), InitialConfigurator.class.getName()));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LoggingSetupBuildItem setupLoggingRuntimeInit(LoggingSetupRecorder recorder, LogConfig log,
            List<LogHandlerBuildItem> handlerBuildItems,
            List<NamedLogHandlersBuildItem> namedHandlerBuildItems, List<LogConsoleFormatBuildItem> consoleFormatItems,
            Optional<ConsoleFormatterBannerBuildItem> possibleBannerBuildItem) {
        final List<RuntimeValue<Optional<Handler>>> handlers = handlerBuildItems.stream()
                .map(LogHandlerBuildItem::getHandlerValue)
                .collect(Collectors.toList());
        final List<RuntimeValue<Map<String, Handler>>> namedHandlers = namedHandlerBuildItems.stream()
                .map(NamedLogHandlersBuildItem::getNamedHandlersMap).collect(Collectors.toList());

        ConsoleFormatterBannerBuildItem bannerBuildItem = null;
        RuntimeValue<Optional<Supplier<String>>> possibleSupplier = null;
        if (possibleBannerBuildItem.isPresent()) {
            bannerBuildItem = possibleBannerBuildItem.get();
        }
        if (bannerBuildItem != null) {
            possibleSupplier = bannerBuildItem.getBannerSupplier();
        }
        recorder.initializeLogging(log, handlers, namedHandlers,
                consoleFormatItems.stream().map(LogConsoleFormatBuildItem::getFormatterValue).collect(Collectors.toList()),
                possibleSupplier);
        return new LoggingSetupBuildItem();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupLoggingStaticInit(LoggingSetupRecorder recorder) {
        recorder.initializeLoggingForImageBuild();
    }

    // This is specifically to help out with presentations, to allow an env var to always override this value
    @BuildStep
    void setUpDarkeningDefault(Consumer<RunTimeConfigurationDefaultBuildItem> rtcConsumer) {
        rtcConsumer.accept(new RunTimeConfigurationDefaultBuildItem("quarkus.log.console.darken", "0"));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerMetrics(LogMetricsHandlerRecorder recorder, LogBuildTimeConfig log,
            BuildProducer<MetricsFactoryConsumerBuildItem> metrics,
            BuildProducer<LogHandlerBuildItem> logHandler, Optional<MetricsCapabilityBuildItem> metricsCapability) {
        if (metricsCapability.isPresent() && log.metricsEnabled) {
            recorder.initCounters();
            metrics.produce(new MetricsFactoryConsumerBuildItem(recorder.registerMetrics()));
            logHandler.produce(new LogHandlerBuildItem(recorder.getLogHandler()));
        }
    }
}
