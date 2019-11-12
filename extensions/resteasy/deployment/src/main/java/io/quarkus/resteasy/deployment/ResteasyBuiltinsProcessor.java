package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.runtime.ExceptionMapperRecorder;
import io.quarkus.resteasy.runtime.ForbiddenExceptionMapper;
import io.quarkus.resteasy.runtime.JaxRsSecurityConfig;
import io.quarkus.resteasy.runtime.NotFoundExceptionMapper;
import io.quarkus.resteasy.runtime.UnauthorizedExceptionMapper;
import io.quarkus.resteasy.server.common.deployment.ResteasyDeploymentBuildItem;
import io.quarkus.security.spi.DenyAllEnabledBuildItem;
import io.quarkus.undertow.deployment.StaticResourceFilesBuildItem;

public class ResteasyBuiltinsProcessor {
    @BuildStep
    void setUpDenyAllJaxRs(CombinedIndexBuildItem index,
            JaxRsSecurityConfig config,
            ResteasyDeploymentBuildItem resteasyDeployment,
            BuildProducer<AnnotationsTransformerBuildItem> transformers,
            BuildProducer<DenyAllEnabledBuildItem> denyAllEnabled) {
        if (config.denyJaxRs) {
            denyAllEnabled.produce(new DenyAllEnabledBuildItem());
            DenyJaxRsTransformer transformer = new DenyJaxRsTransformer(resteasyDeployment.getDeployment());
            transformers.produce(new AnnotationsTransformerBuildItem(transformer));
        }
    }

    /**
     * Install the JAX-RS security provider.
     */
    @BuildStep
    void setUpSecurityExceptionMappers(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(UnauthorizedExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(ForbiddenExceptionMapper.class.getName()));
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
