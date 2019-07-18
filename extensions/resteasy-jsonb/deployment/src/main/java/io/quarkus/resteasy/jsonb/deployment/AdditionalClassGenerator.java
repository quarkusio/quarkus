package io.quarkus.resteasy.jsonb.deployment;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.serializer.JsonbSerializer;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.eclipse.yasson.YassonProperties;
import org.eclipse.yasson.internal.serializer.JsonbDateFormatter;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

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

    void generateJsonbContextResolver(ClassOutput classOutput, List<String> generatedSerializers) {
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

                //create the JsonbConfig object
                MethodDescriptor configCtor = MethodDescriptor.ofConstructor(jsonbConfigClass);
                ResultHandle config = clinit.newInstance(configCtor);

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
                if (!generatedSerializers.isEmpty()) {
                    ResultHandle serializersArray = clinit.newArray(JsonbSerializer.class,
                            clinit.load(generatedSerializers.size()));
                    for (int i = 0; i < generatedSerializers.size(); i++) {
                        ResultHandle serializer = clinit
                                .newInstance(MethodDescriptor.ofConstructor(generatedSerializers.get(i)));
                        clinit.writeArrayValue(serializersArray, clinit.load(i), serializer);
                    }
                    clinit.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withSerializers", jsonbConfigClass,
                                    JsonbSerializer[].class),
                            config, serializersArray);
                }

                //create Jsonb from JsonbBuilder#create using the previously created config
                ResultHandle jsonb = clinit.invokeStaticMethod(
                        MethodDescriptor.ofMethod(JsonbBuilder.class, "create", Jsonb.class, jsonbConfigClass), config);

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
