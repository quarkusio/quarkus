package io.quarkus.resteasy.jsonb.deployment;

import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;

import javax.json.bind.Jsonb;
import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.spi.JsonProvider;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.eclipse.yasson.YassonProperties;
import org.eclipse.yasson.internal.JsonbContext;
import org.eclipse.yasson.internal.MappingContext;
import org.eclipse.yasson.internal.serializer.ContainerSerializerProvider;
import org.eclipse.yasson.internal.serializer.JsonbDateFormatter;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.jsonb.runtime.serializers.QuarkusJsonbBinding;
import io.quarkus.resteasy.jsonb.runtime.serializers.SimpleContainerSerializerProvider;

public class AdditionalClassGenerator {

    public static final String QUARKUS_CONTEXT_RESOLVER = "io.quarkus.jsonb.QuarkusJsonbContextResolver";
    public static final String QUARKUS_DEFAULT_DATE_FORMATTER_PROVIDER = "io.quarkus.jsonb.QuarkusDefaultJsonbDateFormatterProvider";
    public static final String QUARKUS_DEFAULT_LOCALE_PROVIDER = "io.quarkus.jsonb.QuarkusDefaultJsonbLocaleProvider";

    private final JsonbConfig jsonbConfig;

    public AdditionalClassGenerator(JsonbConfig jsonbConfig) {
        this.jsonbConfig = jsonbConfig;
    }

    void generateDefaultLocaleProvider(ClassOutput classOutput) {
        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput).className(QUARKUS_DEFAULT_LOCALE_PROVIDER)
                .build()) {

            FieldDescriptor instance = cc.getFieldCreator("INSTANCE", Locale.class)
                    .setModifiers(Modifier.FINAL | Modifier.STATIC | Modifier.PRIVATE)
                    .getFieldDescriptor();

            try (MethodCreator clinit = cc.getMethodCreator("<clinit>", void.class).setModifiers(Modifier.STATIC)) {

                ResultHandle locale;
                if (jsonbConfig.locale.isPresent()) {
                    locale = clinit.invokeStaticMethod(
                            MethodDescriptor.ofMethod(Locale.class, "forLanguageTag", Locale.class, String.class),
                            clinit.load(jsonbConfig.locale.get()));
                } else {
                    locale = clinit.invokeStaticMethod(
                            MethodDescriptor.ofMethod(Locale.class, "getDefault", Locale.class));
                }

                clinit.writeStaticField(instance, locale);
                clinit.returnValue(null);
            }

            try (MethodCreator get = cc.getMethodCreator("get", Locale.class)) {
                get.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

                get.returnValue(get.readStaticField(instance));
            }
        }
    }

    void generateJsonbDefaultJsonbDateFormatterProvider(ClassOutput classOutput) {
        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput).className(QUARKUS_DEFAULT_DATE_FORMATTER_PROVIDER)
                .build()) {

            FieldDescriptor instance = cc.getFieldCreator("INSTANCE", JsonbDateFormatter.class)
                    .setModifiers(Modifier.FINAL | Modifier.STATIC | Modifier.PRIVATE)
                    .getFieldDescriptor();

            try (MethodCreator clinit = cc.getMethodCreator("<clinit>", void.class).setModifiers(Modifier.STATIC)) {

                ResultHandle locale = clinit.invokeStaticMethod(
                        MethodDescriptor.ofMethod(QUARKUS_DEFAULT_LOCALE_PROVIDER, "get", Locale.class));

                ResultHandle format = clinit.load(jsonbConfig.dateFormat.orElse(JsonbDateFormat.DEFAULT_FORMAT));

                ResultHandle jsonbDateFormatter = clinit.newInstance(
                        MethodDescriptor.ofConstructor(JsonbDateFormatter.class, String.class, String.class),
                        format, locale);

                clinit.writeStaticField(instance, jsonbDateFormatter);
                clinit.returnValue(null);
            }

            try (MethodCreator get = cc.getMethodCreator("get", JsonbDateFormatter.class)) {
                get.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

                get.returnValue(get.readStaticField(instance));
            }
        }
    }

    void generateJsonbContextResolver(ClassOutput classOutput, Map<String, String> typeToGeneratedSerializers) {
        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput).className(QUARKUS_CONTEXT_RESOLVER)
                .interfaces(ContextResolver.class)
                .signature("Ljava/lang/Object;Ljavax/ws/rs/ext/ContextResolver<Ljavax/json/bind/Jsonb;>;")
                .build()) {

            cc.addAnnotation(Provider.class);

            FieldDescriptor instance = cc.getFieldCreator("INSTANCE", Jsonb.class)
                    .setModifiers(Modifier.FINAL | Modifier.STATIC | Modifier.PRIVATE)
                    .getFieldDescriptor();

            try (MethodCreator clinit = cc.getMethodCreator("<clinit>", void.class).setModifiers(Modifier.STATIC)) {
                final Class<javax.json.bind.JsonbConfig> jsonbConfigClass = javax.json.bind.JsonbConfig.class;

                // create the JsonbConfig object
                ResultHandle config = clinit.newInstance(MethodDescriptor.ofConstructor(jsonbConfigClass));

                // create the jsonbContext object
                ResultHandle provider = clinit
                        .invokeStaticMethod(MethodDescriptor.ofMethod(JsonProvider.class, "provider", JsonProvider.class));
                ResultHandle jsonbContext = clinit.newInstance(
                        MethodDescriptor.ofConstructor(JsonbContext.class, jsonbConfigClass, JsonProvider.class),
                        config, provider);
                ResultHandle mappingContext = clinit.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(JsonbContext.class, "getMappingContext", MappingContext.class),
                        jsonbContext);

                //handle locale
                ResultHandle locale = null;
                if (jsonbConfig.locale.isPresent()) {
                    locale = clinit.invokeStaticMethod(
                            MethodDescriptor.ofMethod(QUARKUS_DEFAULT_LOCALE_PROVIDER, "get", Locale.class));
                    clinit.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withLocale", jsonbConfigClass, Locale.class),
                            config, locale);
                }

                // handle date format
                if (jsonbConfig.dateFormat.isPresent()) {
                    clinit.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withDateFormat", jsonbConfigClass, String.class,
                                    Locale.class),
                            config,
                            clinit.load(jsonbConfig.dateFormat.get()),
                            locale != null ? locale : clinit.loadNull());
                }

                // handle serializeNullValues
                clinit.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(jsonbConfigClass, "withNullValues", jsonbConfigClass, Boolean.class),
                        config,
                        clinit.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Boolean.class, "valueOf", Boolean.class, boolean.class),
                                clinit.load(jsonbConfig.serializeNullValues)));

                // handle propertyOrderStrategy
                clinit.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(jsonbConfigClass, "withPropertyOrderStrategy", jsonbConfigClass,
                                String.class),
                        config, clinit.load(jsonbConfig.propertyOrderStrategy.toUpperCase()));

                // handle encoding
                if (jsonbConfig.encoding.isPresent()) {
                    clinit.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withEncoding", jsonbConfigClass,
                                    String.class),
                            config, clinit.load(jsonbConfig.encoding.get()));
                }

                // handle failOnUnknownProperties
                clinit.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(jsonbConfigClass, "setProperty", jsonbConfigClass, String.class,
                                Object.class),
                        config,
                        clinit.load(YassonProperties.FAIL_ON_UNKNOWN_PROPERTIES),
                        clinit.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Boolean.class, "valueOf", Boolean.class, boolean.class),
                                clinit.load(jsonbConfig.failOnUnknownProperties)));

                // add generated serializers to config
                if (!typeToGeneratedSerializers.isEmpty()) {
                    ResultHandle serializersArray = clinit.newArray(JsonbSerializer.class,
                            clinit.load(typeToGeneratedSerializers.size()));
                    int i = 0;
                    for (Map.Entry<String, String> entry : typeToGeneratedSerializers.entrySet()) {

                        ResultHandle serializer = clinit
                                .newInstance(MethodDescriptor.ofConstructor(entry.getValue()));

                        // build up the serializers array that will be passed to JsonbConfig
                        clinit.writeArrayValue(serializersArray, clinit.load(i), serializer);

                        ResultHandle clazz = clinit.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Class.class, "forName", Class.class, String.class),
                                clinit.load(entry.getKey()));

                        // add a ContainerSerializerProvider for the serializer
                        ResultHandle serializerProvider = clinit.newInstance(
                                MethodDescriptor.ofConstructor(SimpleContainerSerializerProvider.class, JsonbSerializer.class),
                                serializer);
                        clinit.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(MappingContext.class, "addSerializerProvider", void.class,
                                        Class.class, ContainerSerializerProvider.class),
                                mappingContext, clazz, serializerProvider);

                        i++;
                    }
                    clinit.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withSerializers", jsonbConfigClass,
                                    JsonbSerializer[].class),
                            config, serializersArray);
                }

                // create jsonb from QuarkusJsonbBinding
                ResultHandle jsonb = clinit.newInstance(
                        MethodDescriptor.ofConstructor(QuarkusJsonbBinding.class, JsonbContext.class), jsonbContext);
                clinit.writeStaticField(instance, jsonb);

                clinit.returnValue(null);
            }

            try (MethodCreator getContext = cc.getMethodCreator("getContext", Jsonb.class, Class.class)) {
                getContext.returnValue(getContext.readStaticField(instance));
            }

            try (MethodCreator bridgeGetContext = cc.getMethodCreator("getContext", Object.class, Class.class)) {
                bridgeGetContext.returnValue(bridgeGetContext.readStaticField(instance));
            }
        }
    }
}
