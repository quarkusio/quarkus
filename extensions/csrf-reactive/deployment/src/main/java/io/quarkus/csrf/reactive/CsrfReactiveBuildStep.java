package io.quarkus.csrf.reactive;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.server.model.FixedHandlersChainCustomizer;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.csrf.reactive.runtime.CsrfHandler;
import io.quarkus.csrf.reactive.runtime.CsrfReactiveConfig;
import io.quarkus.csrf.reactive.runtime.CsrfRecorder;
import io.quarkus.csrf.reactive.runtime.CsrfResponseFilter;
import io.quarkus.csrf.reactive.runtime.CsrfTokenParameterProvider;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.server.spi.HandlerConfigurationProviderBuildItem;
import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;

@BuildSteps(onlyIf = CsrfReactiveBuildStep.IsEnabled.class)
public class CsrfReactiveBuildStep {

    @BuildStep
    void registerProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItem) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(CsrfResponseFilter.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, CsrfResponseFilter.class));
        additionalIndexedClassesBuildItem
                .produce(new AdditionalIndexedClassesBuildItem(CsrfResponseFilter.class.getName()));

        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(CsrfTokenParameterProvider.class));
    }

    @BuildStep
    public MethodScannerBuildItem configureHandler() {
        return new MethodScannerBuildItem(new MethodScanner() {
            @Override
            public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
                    Map<String, Object> methodContext) {
                return Collections.singletonList(
                        new FixedHandlersChainCustomizer(
                                List.of(new CsrfHandler()),
                                HandlerChainCustomizer.Phase.BEFORE_METHOD_INVOKE));
            }
        });
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public HandlerConfigurationProviderBuildItem applyRuntimeConfig(CsrfRecorder recorder,
            CsrfReactiveConfig csrfReactiveConfig) {
        return new HandlerConfigurationProviderBuildItem(CsrfReactiveConfig.class, recorder.configure(csrfReactiveConfig));
    }

    public static class IsEnabled implements BooleanSupplier {
        CsrfReactiveBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled;
        }
    }
}
