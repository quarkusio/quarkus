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
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConsoleFormatterBannerBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
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
import io.quarkus.gizmo.AnnotationCreator;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
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

    @BuildStep
    void setUpTraceLogging(LogBuildTimeConfig log,
            final BuildProducer<GeneratedClassBuildItem> generatedTraceLogger) {
        final List<String> traceCategories = log.categories.entrySet().stream()
                .filter(entry -> entry.getValue().buildTimeTraceEnabled)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        ClassOutput output = new GeneratedClassGizmoAdaptor(generatedTraceLogger, false);
        if (traceCategories.isEmpty()) {
            generateNoTraceLogger(output);
        } else {
            generateTraceLogger(traceCategories, output);
        }
    }

    private void generateNoTraceLogger(ClassOutput output) {
        try (ClassCreator cc = ClassCreator.builder().setFinal(true)
                .className("Target_org_jboss_logging_Logger")
                .classOutput(output).build()) {

            AnnotationCreator targetClass = cc.addAnnotation("com.oracle.svm.core.annotate.TargetClass");
            targetClass.addValue("className", "org.jboss.logging.Logger");

            MethodCreator isTraceEnabled = cc.getMethodCreator("isTraceEnabled", boolean.class);
            isTraceEnabled.addAnnotation("com.oracle.svm.core.annotate.Substitute");
            isTraceEnabled.addAnnotation("org.graalvm.compiler.api.replacements.Fold");

            isTraceEnabled.returnValue(isTraceEnabled.load(false));
        }
    }

    private void generateTraceLogger(List<String> categories, ClassOutput output) {
        try (ClassCreator cc = ClassCreator.builder().setFinal(true)
                .className("Target_org_jboss_logging_Logger")
                .classOutput(output).build()) {

            AnnotationCreator targetClass = cc.addAnnotation("com.oracle.svm.core.annotate.TargetClass");
            targetClass.addValue("className", "org.jboss.logging.Logger");

            final FieldCreator nameAlias = cc.getFieldCreator("name", String.class);
            nameAlias.addAnnotation("com.oracle.svm.core.annotate.Alias");

            MethodCreator isTraceEnabled = cc.getMethodCreator("isTraceEnabled", boolean.class);
            isTraceEnabled.addAnnotation("com.oracle.svm.core.annotate.Substitute");
            isTraceEnabled.addAnnotation("org.graalvm.compiler.api.replacements.Fold");

            final FieldDescriptor nameAliasField = nameAlias.getFieldDescriptor();
            BytecodeCreator current = isTraceEnabled;
            for (String category : categories) {
                ResultHandle equalsResult = current.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Object.class, "equals", boolean.class, Object.class),
                        current.readInstanceField(nameAliasField, current.getThis()), current.load(category));

                BranchResult equalsBranch = current.ifTrue(equalsResult);
                try (BytecodeCreator false1 = equalsBranch.falseBranch()) {
                    ResultHandle startsWithResult = false1.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(String.class, "startsWith", boolean.class, String.class),
                            false1.readInstanceField(nameAliasField, false1.getThis()), false1.load(category));

                    BranchResult startsWithBranch = false1.ifTrue(startsWithResult);
                    startsWithBranch.trueBranch().returnValue(startsWithBranch.trueBranch().load(true));
                    current = startsWithBranch.falseBranch();
                }

                equalsBranch.trueBranch().returnValue(equalsBranch.trueBranch().load(true));
            }

            current.returnValue(current.load(false));
        }
    }
}
