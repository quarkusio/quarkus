package io.quarkus.resteasy.reactive.qute.deployment;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COMPLETION_STAGE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.UNI;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.server.handlers.UniResponseHandler;
import org.jboss.resteasy.reactive.server.model.FixedHandlersChainCustomizer;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.resteasy.reactive.qute.runtime.TemplateResponseFilter;
import io.quarkus.resteasy.reactive.qute.runtime.TemplateResponseUniHandler;
import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;
import io.quarkus.resteasy.reactive.server.spi.NonBlockingReturnTypeBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomContainerResponseFilterBuildItem;

public class ResteasyReactiveQuteProcessor {

    private static final DotName TEMPLATE_INSTANCE = DotName.createSimple(TemplateInstance.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.REST_QUTE);
    }

    @BuildStep
    CustomContainerResponseFilterBuildItem registerProviders() {
        return new CustomContainerResponseFilterBuildItem(TemplateResponseFilter.class.getName());
    }

    @BuildStep
    ReflectiveHierarchyIgnoreWarningBuildItem ignoreReflectiveWarning() {
        return new ReflectiveHierarchyIgnoreWarningBuildItem(
                new ReflectiveHierarchyIgnoreWarningBuildItem.DotNameExclusion(TEMPLATE_INSTANCE));
    }

    @BuildStep
    void nonBlockingTemplateInstance(RestQuteConfig config, BuildProducer<NonBlockingReturnTypeBuildItem> nonBlockingType) {
        if (config.templateInstanceNonBlockingType()) {
            nonBlockingType.produce(new NonBlockingReturnTypeBuildItem(TEMPLATE_INSTANCE));
        }
    }

    @BuildStep
    public MethodScannerBuildItem configureHandler() {
        return new MethodScannerBuildItem(new MethodScanner() {
            @Override
            public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
                    Map<String, Object> methodContext) {
                if (method.returnType().name().equals(TEMPLATE_INSTANCE) || isAsyncTemplateInstance(method.returnType())) {
                    // TemplateResponseUniHandler creates a Uni, so we also need to introduce another Uni handler
                    // so RR actually gets the result
                    // the reason why we use AFTER_METHOD_INVOKE_SECOND_ROUND is to be able to properly support Uni<TemplateInstance>
                    return Collections.singletonList(
                            new FixedHandlersChainCustomizer(
                                    List.of(new TemplateResponseUniHandler(), new UniResponseHandler()),
                                    HandlerChainCustomizer.Phase.AFTER_METHOD_INVOKE_SECOND_ROUND));
                }
                return Collections.emptyList();
            }

            private boolean isAsyncTemplateInstance(Type type) {
                boolean isAsyncTemplateInstance = false;
                if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                    ParameterizedType parameterizedType = type.asParameterizedType();
                    if ((parameterizedType.name().equals(UNI) || parameterizedType.name().equals(COMPLETION_STAGE))
                            && (parameterizedType.arguments().size() == 1)) {
                        DotName firstParameterType = parameterizedType.arguments().get(0).name();
                        isAsyncTemplateInstance = firstParameterType.equals(TEMPLATE_INSTANCE);
                    }
                }
                return isAsyncTemplateInstance;
            }
        });
    }
}
