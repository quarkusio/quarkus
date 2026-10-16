package io.quarkus.logging.json.deployment;

import java.io.IOException;
import java.util.List;
import java.util.logging.Formatter;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogNamedHandlerFormatBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.logging.json.runtime.JsonLogConfig;
import io.quarkus.logging.json.runtime.JsonProvider;
import io.quarkus.logging.json.runtime.LoggingJsonRecorder;
import io.quarkus.runtime.ApplicationConfig;

public final class LoggingJsonProcessor {

    @BuildStep
    void setUpFormatters(ServiceRegistrar serviceRegistrar,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) throws IOException {
        List<String> providerClassNames = providerClassNames();
        if (!providerClassNames.isEmpty()) {
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(providerClassNames.toArray(String[]::new)).build());
        }
        serviceRegistrar.forService(Formatter.class, "console")
                .atPhase(Phase.LOGGING)
                .require(JsonLogConfig.class)
                .require(ApplicationConfig.class)
                .optional()
                .onStart((ctx, config, applicationConfig) -> LoggingJsonRecorder.createFormatter(config.consoleJson(),
                        providerClassNames, applicationConfig));
        serviceRegistrar.forService(Formatter.class, "file")
                .atPhase(Phase.LOGGING)
                .require(JsonLogConfig.class)
                .require(ApplicationConfig.class)
                .optional()
                .onStart((ctx, config, applicationConfig) -> LoggingJsonRecorder.createFormatter(config.fileJson(),
                        providerClassNames, applicationConfig));
        serviceRegistrar.forService(Formatter.class, "syslog")
                .atPhase(Phase.LOGGING)
                .require(JsonLogConfig.class)
                .require(ApplicationConfig.class)
                .optional()
                .onStart((ctx, config, applicationConfig) -> LoggingJsonRecorder.createFormatter(config.syslogJson(),
                        providerClassNames, applicationConfig));
        serviceRegistrar.forService(Formatter.class, "socket")
                .atPhase(Phase.LOGGING)
                .require(JsonLogConfig.class)
                .require(ApplicationConfig.class)
                .optional()
                .onStart((ctx, config, applicationConfig) -> LoggingJsonRecorder.createFormatter(config.socketJson(),
                        providerClassNames, applicationConfig));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setUpNamedHandlerFormatters(LoggingJsonRecorder recorder,
            BuildProducer<LogNamedHandlerFormatBuildItem> namedBuildItem) throws IOException {
        recorder.initializeJsonProviders(providerClassNames());
        namedBuildItem.produce(new LogNamedHandlerFormatBuildItem(recorder.initializeNamedJsonLogging()));
    }

    @BuildStep
    void setUpUnremovableProviders(Capabilities capabilities, BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        if (capabilities.isPresent(Capability.CDI)) {
            unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(JsonProvider.class));
        }
    }

    private static List<String> providerClassNames() throws IOException {
        return List.copyOf(ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                ServiceProviderBuildItem.SPI_ROOT + JsonProvider.class.getName()));
    }
}
