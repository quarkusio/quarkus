package io.quarkus.logging.json.deployment;

import java.io.IOException;
import java.util.List;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogConsoleFormatBuildItem;
import io.quarkus.deployment.builditem.LogFileFormatBuildItem;
import io.quarkus.deployment.builditem.LogNamedHandlerFormatBuildItem;
import io.quarkus.deployment.builditem.LogSocketFormatBuildItem;
import io.quarkus.deployment.builditem.LogSyslogFormatBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.logging.json.runtime.JsonProvider;
import io.quarkus.logging.json.runtime.LoggingJsonRecorder;

public final class LoggingJsonProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setUpFormatters(LoggingJsonRecorder recorder,
            BuildProducer<LogConsoleFormatBuildItem> consoleBuildItem,
            BuildProducer<LogFileFormatBuildItem> fileBuildItem,
            BuildProducer<LogSyslogFormatBuildItem> syslogBuildItem,
            BuildProducer<LogSocketFormatBuildItem> socketBuildItem,
            BuildProducer<LogNamedHandlerFormatBuildItem> namedBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) throws IOException {
        List<String> providerClassNames = List.copyOf(ServiceUtil.classNamesNamedIn(
                Thread.currentThread().getContextClassLoader(),
                ServiceProviderBuildItem.SPI_ROOT + JsonProvider.class.getName()));
        if (!providerClassNames.isEmpty()) {
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(providerClassNames.toArray(String[]::new)).build());
        }
        recorder.initializeJsonProviders(providerClassNames);
        consoleBuildItem.produce(new LogConsoleFormatBuildItem(recorder.initializeConsoleJsonLogging()));
        fileBuildItem.produce(new LogFileFormatBuildItem(recorder.initializeFileJsonLogging()));
        syslogBuildItem.produce(new LogSyslogFormatBuildItem(recorder.initializeSyslogJsonLogging()));
        socketBuildItem.produce(new LogSocketFormatBuildItem(recorder.initializeSocketJsonLogging()));
        namedBuildItem.produce(new LogNamedHandlerFormatBuildItem(recorder.initializeNamedJsonLogging()));
    }

    @BuildStep
    void setUpUnremovableProviders(Capabilities capabilities, BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        if (capabilities.isPresent(Capability.CDI)) {
            unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(JsonProvider.class));
        }
    }
}
