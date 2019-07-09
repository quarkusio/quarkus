package io.quarkus.resteasy.jsonb.deployment;

import java.util.Collection;
import java.util.Locale;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.eclipse.yasson.YassonProperties;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.common.deployment.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.server.common.deployment.ResteasyServerCommonProcessor;

public class ResteasyJsonbProcessor {

    private static final String CONTEXT_RESOLVER = "io.quarkus.jsonb.QuarkusJsonbContextResolver";

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY_JSONB));
    }

    JsonbConfig jsonbConfig;

    @BuildStep
    void generateJsonbContextResolver(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ResteasyJaxrsProviderBuildItem> jaxrsProvider) {
        IndexView index = combinedIndexBuildItem.getIndex();

        validateConfiguration();

        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedClass.produce(new GeneratedClassBuildItem(true, name, data));
            }
        };

        for (DotName annotationType : ResteasyServerCommonProcessor.METHOD_ANNOTATIONS) {
            Collection<AnnotationInstance> instances = index.getAnnotations(annotationType);
            for (AnnotationInstance instance : instances) {
                MethodInfo method = instance.target().asMethod();
                if (ResteasyServerCommonProcessor.isReflectionDeclarationRequiredFor(method.returnType())) {
                    // TODO generate a serializer
                }
            }
        }

        generateJsonbContextResolver(classOutput);
        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(CONTEXT_RESOLVER));
    }

    private void validateConfiguration() {
        if (!jsonbConfig.isValidPropertyOrderStrategy()) {
            throw new IllegalArgumentException(
                    "quarkus.jsonb.property-order-strategy can only be one of " + JsonbConfig.ALLOWED_PROPERTY_ORDER_VALUES);
        }
    }

    private void generateJsonbContextResolver(ClassOutput classOutput) {
        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput).className(CONTEXT_RESOLVER)
                .interfaces(ContextResolver.class)
                .signature("Ljava/lang/Object;Ljavax/ws/rs/ext/ContextResolver<Ljavax/json/bind/Jsonb;>;")
                .build()) {

            cc.addAnnotation(Provider.class);

            try (MethodCreator getContext = cc.getMethodCreator("getContext", Jsonb.class, Class.class)) {
                final Class<javax.json.bind.JsonbConfig> jsonbConfigClass = javax.json.bind.JsonbConfig.class;

                //create the JsonbConfig object
                MethodDescriptor configCtor = MethodDescriptor.ofConstructor(jsonbConfigClass);
                ResultHandle config = getContext.newInstance(configCtor);

                //handle locale
                ResultHandle locale = null;
                if (!jsonbConfig.locale.isEmpty()) {
                    locale = getContext.invokeStaticMethod(
                            MethodDescriptor.ofMethod(Locale.class, "forLanguageTag", Locale.class, String.class),
                            getContext.load(jsonbConfig.locale));
                    getContext.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withLocale", jsonbConfigClass, Locale.class),
                            config, locale);
                }

                // handle date format
                if (!jsonbConfig.dateFormat.isEmpty()) {
                    getContext.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withDateFormat", jsonbConfigClass, String.class,
                                    Locale.class),
                            config,
                            getContext.load(jsonbConfig.dateFormat),
                            locale != null ? locale : getContext.loadNull());
                }

                // handle serializeNullValues
                getContext.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(jsonbConfigClass, "withNullValues", jsonbConfigClass, Boolean.class),
                        config,
                        getContext.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Boolean.class, "valueOf", Boolean.class, boolean.class),
                                getContext.load(jsonbConfig.serializeNullValues)));

                // handle propertyOrderStrategy
                getContext.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(jsonbConfigClass, "withPropertyOrderStrategy", jsonbConfigClass,
                                String.class),
                        config, getContext.load(jsonbConfig.propertyOrderStrategy.toUpperCase()));

                // handle encoding
                if (!jsonbConfig.encoding.isEmpty()) {
                    getContext.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withEncoding", jsonbConfigClass,
                                    String.class),
                            config, getContext.load(jsonbConfig.encoding));
                }

                // handle failOnUnknownProperties
                getContext.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(jsonbConfigClass, "setProperty", jsonbConfigClass, String.class,
                                Object.class),
                        config,
                        getContext.load(YassonProperties.FAIL_ON_UNKNOWN_PROPERTIES),
                        getContext.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Boolean.class, "valueOf", Boolean.class, boolean.class),
                                getContext.load(jsonbConfig.failOnUnknownProperties)));

                //create Jsonb from JsonbBuilder#create using the previously created config
                ResultHandle result = getContext.invokeStaticMethod(
                        MethodDescriptor.ofMethod(JsonbBuilder.class, "create", Jsonb.class, jsonbConfigClass), config);
                getContext.returnValue(result);
            }

            try (MethodCreator bridgeGetContext = cc.getMethodCreator("getContext", Object.class, Class.class)) {
                MethodDescriptor getContext = MethodDescriptor.ofMethod(CONTEXT_RESOLVER, "getContext", "javax.json.bind.Jsonb",
                        "java.lang.Class");
                ResultHandle result = bridgeGetContext.invokeVirtualMethod(getContext, bridgeGetContext.getThis(),
                        bridgeGetContext.getMethodParam(0));
                bridgeGetContext.returnValue(result);
            }
        }
    }

}
