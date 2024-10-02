package io.quarkus.maven;

import static io.quarkus.analytics.dto.segment.ContextBuilder.CommonSystemProperties.MAVEN_VERSION;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.rtinfo.RuntimeInformation;

import io.quarkus.analytics.AnalyticsService;
import io.quarkus.analytics.config.FileLocationsImpl;
import io.quarkus.analytics.dto.segment.TrackEventType;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.platform.tools.maven.MojoMessageWriter;

@Singleton
@Named
public class BuildAnalyticsProvider {

    private final RuntimeInformation runtimeInformation;

    private final AnalyticsService analyticsService;

    private Log log;

    @Inject
    public BuildAnalyticsProvider(RuntimeInformation runtimeInformation) {
        this.runtimeInformation = runtimeInformation;
        analyticsService = new AnalyticsService(FileLocationsImpl.INSTANCE, new MojoMessageWriter(getLog()));
    }

    public void sendAnalytics(TrackEventType trackEventType,
            ApplicationModel applicationModel,
            Map<String, String> graalVMInfo,
            File localBuildDir) {
        final long start = System.nanoTime();

        final Map<String, Object> buildInfo = new HashMap<>(graalVMInfo);
        buildInfo.put(MAVEN_VERSION, runtimeInformation.getMavenVersion());
        analyticsService.sendAnalytics(trackEventType, applicationModel, buildInfo, localBuildDir);

        if (getLog().isDebugEnabled()) {
            getLog().debug("Analytics took " + (duration(System.nanoTime(), start)) + "ms");
        }
    }

    private long duration(long end, long start) {
        return TimeUnit.MILLISECONDS.convert(end - start, TimeUnit.NANOSECONDS);
    }

    public void buildAnalyticsUserInput(Function<String, String> analyticsEnabledSupplier) {
        analyticsService.buildAnalyticsUserInput(analyticsEnabledSupplier);
    }

    public void close() {
        analyticsService.close();
    }

    private Log getLog() {
        if (log == null) {
            log = new SystemStreamLog();
        }
        return log;
    }
}
