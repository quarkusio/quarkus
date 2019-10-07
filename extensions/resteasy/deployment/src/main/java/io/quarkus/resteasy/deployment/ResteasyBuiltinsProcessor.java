package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.runtime.ExceptionMapperRecorder;
import io.quarkus.resteasy.runtime.NotFoundExceptionMapper;
import io.quarkus.resteasy.runtime.RolesFilterRegistrar;
import io.quarkus.undertow.deployment.StaticResourceFilesBuildItem;

public class ResteasyBuiltinsProcessor {
    /**
     * Install the JAX-RS security provider.
     */
    @BuildStep
    void setupFilter(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(RolesFilterRegistrar.class.getName()));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void setupExceptionMapper(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(NotFoundExceptionMapper.class.getName()));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(STATIC_INIT)
    void addStaticResourcesExceptionMapper(StaticResourceFilesBuildItem paths, ExceptionMapperRecorder recorder) {
        //limit to 1000 to not have to many files to display
        Set<String> staticResources = paths.files.stream().filter(this::isHtmlFileName).limit(1000).collect(Collectors.toSet());
        if (staticResources.isEmpty()) {
            staticResources = paths.files.stream().limit(1000).collect(Collectors.toSet());
        }
        recorder.setStaticResource(staticResources);
    }

    private boolean isHtmlFileName(String fileName) {
        return fileName.endsWith(".html") || fileName.endsWith(".htm");
    }

}
