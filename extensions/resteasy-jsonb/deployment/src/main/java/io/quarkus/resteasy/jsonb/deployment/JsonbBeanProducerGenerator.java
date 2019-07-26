package io.quarkus.resteasy.jsonb.deployment;

import java.util.Locale;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.json.bind.Jsonb;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.spi.JsonProvider;

import org.eclipse.yasson.YassonProperties;
import org.eclipse.yasson.internal.JsonbContext;
import org.eclipse.yasson.internal.MappingContext;
import org.eclipse.yasson.internal.serializer.ContainerSerializerProvider;

import io.quarkus.arc.DefaultBean;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.jsonb.runtime.serializers.QuarkusJsonbBinding;
import io.quarkus.resteasy.jsonb.runtime.serializers.SimpleContainerSerializerProvider;

public class JsonbBeanProducerGenerator {

    public static String JSONB_PRODUCER = "io.quarkus.jsonb.JsonbProducer";

    private final JsonbConfig jsonbConfig;

    public JsonbBeanProducerGenerator(JsonbConfig jsonbConfig) {
        this.jsonbConfig = jsonbConfig;
    }

    void generateJsonbContextResolver(ClassOutput classOutput, Map<String, String> typeToGeneratedSerializers) {
        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput).className(JSONB_PRODUCER)
                .build()) {

            cc.addAnnotation(ApplicationScoped.class);

            try (MethodCreator createJsonb = cc.getMethodCreator("createJsonb", Jsonb.class)) {

                createJsonb.addAnnotation(Singleton.class);
                createJsonb.addAnnotation(Produces.class);
                createJsonb.addAnnotation(DefaultBean.class);

                Class<javax.json.bind.JsonbConfig> jsonbConfigClass = javax.json.bind.JsonbConfig.class;

                // create the JsonbConfig object
                ResultHandle config = createJsonb.newInstance(MethodDescriptor.ofConstructor(jsonbConfigClass));

                // create the jsonbContext object
                ResultHandle provider = createJsonb
                        .invokeStaticMethod(MethodDescriptor.ofMethod(JsonProvider.class, "provider", JsonProvider.class));
                ResultHandle jsonbContext = createJsonb.newInstance(
                        MethodDescriptor.ofConstructor(JsonbContext.class, jsonbConfigClass, JsonProvider.class),
                        config, provider);
                ResultHandle mappingContext = createJsonb.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(JsonbContext.class, "getMappingContext", MappingContext.class),
                        jsonbContext);

                //handle locale
                ResultHandle locale = null;
                if (jsonbConfig.locale.isPresent()) {
                    locale = createJsonb.invokeStaticMethod(
                            MethodDescriptor.ofMethod(JsonbSupportClassGenerator.QUARKUS_DEFAULT_LOCALE_PROVIDER, "get",
                                    Locale.class));
                    createJsonb.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withLocale", jsonbConfigClass, Locale.class),
                            config, locale);
                }

                // handle date format
                if (jsonbConfig.dateFormat.isPresent()) {
                    createJsonb.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withDateFormat", jsonbConfigClass, String.class,
                                    Locale.class),
                            config,
                            createJsonb.load(jsonbConfig.dateFormat.get()),
                            locale != null ? locale : createJsonb.loadNull());
                }

                // handle serializeNullValues
                createJsonb.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(jsonbConfigClass, "withNullValues", jsonbConfigClass, Boolean.class),
                        config,
                        createJsonb.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Boolean.class, "valueOf", Boolean.class, boolean.class),
                                createJsonb.load(jsonbConfig.serializeNullValues)));

                // handle propertyOrderStrategy
                createJsonb.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(jsonbConfigClass, "withPropertyOrderStrategy", jsonbConfigClass,
                                String.class),
                        config, createJsonb.load(jsonbConfig.propertyOrderStrategy.toUpperCase()));

                // handle encoding
                if (jsonbConfig.encoding.isPresent()) {
                    createJsonb.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withEncoding", jsonbConfigClass,
                                    String.class),
                            config, createJsonb.load(jsonbConfig.encoding.get()));
                }

                // handle failOnUnknownProperties
                createJsonb.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(jsonbConfigClass, "setProperty", jsonbConfigClass, String.class,
                                Object.class),
                        config,
                        createJsonb.load(YassonProperties.FAIL_ON_UNKNOWN_PROPERTIES),
                        createJsonb.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Boolean.class, "valueOf", Boolean.class, boolean.class),
                                createJsonb.load(jsonbConfig.failOnUnknownProperties)));

                // add generated serializers to config
                if (!typeToGeneratedSerializers.isEmpty()) {
                    ResultHandle serializersArray = createJsonb.newArray(JsonbSerializer.class,
                            createJsonb.load(typeToGeneratedSerializers.size()));
                    int i = 0;
                    for (Map.Entry<String, String> entry : typeToGeneratedSerializers.entrySet()) {

                        ResultHandle serializer = createJsonb
                                .newInstance(MethodDescriptor.ofConstructor(entry.getValue()));

                        // build up the serializers array that will be passed to JsonbConfig
                        createJsonb.writeArrayValue(serializersArray, createJsonb.load(i), serializer);

                        ResultHandle clazz = createJsonb.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Class.class, "forName", Class.class, String.class),
                                createJsonb.load(entry.getKey()));

                        // add a ContainerSerializerProvider for the serializer
                        ResultHandle serializerProvider = createJsonb.newInstance(
                                MethodDescriptor.ofConstructor(SimpleContainerSerializerProvider.class, JsonbSerializer.class),
                                serializer);
                        createJsonb.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(MappingContext.class, "addSerializerProvider", void.class,
                                        Class.class, ContainerSerializerProvider.class),
                                mappingContext, clazz, serializerProvider);

                        i++;
                    }
                    createJsonb.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withSerializers", jsonbConfigClass,
                                    JsonbSerializer[].class),
                            config, serializersArray);
                }

                // create jsonb from QuarkusJsonbBinding
                ResultHandle jsonb = createJsonb.newInstance(
                        MethodDescriptor.ofConstructor(QuarkusJsonbBinding.class, JsonbContext.class), jsonbContext);

                createJsonb.returnValue(jsonb);
            }
        }
    }
}
