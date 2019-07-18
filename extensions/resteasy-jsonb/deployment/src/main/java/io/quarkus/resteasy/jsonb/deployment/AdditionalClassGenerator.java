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

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
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
                    .setModifiers(Modifier.STATIC | Modifier.PRIVATE)
                    .getFieldDescriptor();

            try (MethodCreator get = cc.getMethodCreator("get", Locale.class)) {
                get.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

                BranchResult branchResult = get.ifNull(get.readStaticField(instance));

                BytecodeCreator instanceNotNull = branchResult.falseBranch();
                instanceNotNull.returnValue(instanceNotNull.readStaticField(instance));

                BytecodeCreator instanceNull = branchResult.trueBranch();
                ResultHandle locale;
                if (jsonbConfig.locale.isPresent()) {
                    locale = instanceNull.invokeStaticMethod(
                            MethodDescriptor.ofMethod(Locale.class, "forLanguageTag", Locale.class, String.class),
                            instanceNull.load(jsonbConfig.locale.get()));
                } else {
                    locale = instanceNull.invokeStaticMethod(
                            MethodDescriptor.ofMethod(Locale.class, "getDefault", Locale.class));
                }

                instanceNull.writeStaticField(instance, locale);
                instanceNull.returnValue(locale);
            }
        }
    }

    void generateJsonbDefaultJsonbDateFormatterProvider(ClassOutput classOutput) {
        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput).className(QUARKUS_DEFAULT_DATE_FORMATTER_PROVIDER)
                .build()) {

            FieldDescriptor instance = cc.getFieldCreator("INSTANCE", JsonbDateFormatter.class)
                    .setModifiers(Modifier.STATIC | Modifier.PRIVATE)
                    .getFieldDescriptor();

            try (MethodCreator get = cc.getMethodCreator("get", JsonbDateFormatter.class)) {
                get.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

                BranchResult branchResult = get.ifNull(get.readStaticField(instance));

                BytecodeCreator instanceNotNull = branchResult.falseBranch();
                instanceNotNull.returnValue(instanceNotNull.readStaticField(instance));

                BytecodeCreator instanceNull = branchResult.trueBranch();
                ResultHandle locale = instanceNull.invokeStaticMethod(
                        MethodDescriptor.ofMethod(QUARKUS_DEFAULT_LOCALE_PROVIDER, "get", Locale.class));

                ResultHandle localeStr = instanceNull.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Locale.class, "toLanguageTag", String.class),
                        locale);

                ResultHandle format = instanceNull.load(jsonbConfig.dateFormat.orElse(JsonbDateFormat.DEFAULT_FORMAT));

                ResultHandle jsonbDateFormatter = instanceNull.newInstance(
                        MethodDescriptor.ofConstructor(JsonbDateFormatter.class, String.class, String.class),
                        format, localeStr);
                instanceNull.writeStaticField(instance, jsonbDateFormatter);
                instanceNull.returnValue(jsonbDateFormatter);
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
                    .setModifiers(Modifier.STATIC | Modifier.PRIVATE)
                    .getFieldDescriptor();

            try (MethodCreator getContext = cc.getMethodCreator("getContext", Jsonb.class, Class.class)) {
                BranchResult branchResult = getContext.ifNull(getContext.readStaticField(instance));

                BytecodeCreator instanceNotNull = branchResult.falseBranch();
                instanceNotNull.returnValue(instanceNotNull.readStaticField(instance));

                BytecodeCreator instanceNull = branchResult.trueBranch();

                Class<javax.json.bind.JsonbConfig> jsonbConfigClass = javax.json.bind.JsonbConfig.class;

                // create the JsonbConfig object
                ResultHandle config = instanceNull.newInstance(MethodDescriptor.ofConstructor(jsonbConfigClass));

                // create the jsonbContext object
                ResultHandle provider = instanceNull
                        .invokeStaticMethod(MethodDescriptor.ofMethod(JsonProvider.class, "provider", JsonProvider.class));
                ResultHandle jsonbContext = instanceNull.newInstance(
                        MethodDescriptor.ofConstructor(JsonbContext.class, jsonbConfigClass, JsonProvider.class),
                        config, provider);
                ResultHandle mappingContext = instanceNull.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(JsonbContext.class, "getMappingContext", MappingContext.class),
                        jsonbContext);

                //handle locale
                ResultHandle locale = null;
                if (jsonbConfig.locale.isPresent()) {
                    locale = instanceNull.invokeStaticMethod(
                            MethodDescriptor.ofMethod(QUARKUS_DEFAULT_LOCALE_PROVIDER, "get", Locale.class));
                    instanceNull.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withLocale", jsonbConfigClass, Locale.class),
                            config, locale);
                }

                // handle date format
                if (jsonbConfig.dateFormat.isPresent()) {
                    instanceNull.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withDateFormat", jsonbConfigClass, String.class,
                                    Locale.class),
                            config,
                            instanceNull.load(jsonbConfig.dateFormat.get()),
                            locale != null ? locale : instanceNull.loadNull());
                }

                // handle serializeNullValues
                instanceNull.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(jsonbConfigClass, "withNullValues", jsonbConfigClass, Boolean.class),
                        config,
                        instanceNull.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Boolean.class, "valueOf", Boolean.class, boolean.class),
                                instanceNull.load(jsonbConfig.serializeNullValues)));

                // handle propertyOrderStrategy
                instanceNull.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(jsonbConfigClass, "withPropertyOrderStrategy", jsonbConfigClass,
                                String.class),
                        config, instanceNull.load(jsonbConfig.propertyOrderStrategy.toUpperCase()));

                // handle encoding
                if (jsonbConfig.encoding.isPresent()) {
                    instanceNull.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withEncoding", jsonbConfigClass,
                                    String.class),
                            config, instanceNull.load(jsonbConfig.encoding.get()));
                }

                // handle failOnUnknownProperties
                instanceNull.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(jsonbConfigClass, "setProperty", jsonbConfigClass, String.class,
                                Object.class),
                        config,
                        instanceNull.load(YassonProperties.FAIL_ON_UNKNOWN_PROPERTIES),
                        instanceNull.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Boolean.class, "valueOf", Boolean.class, boolean.class),
                                instanceNull.load(jsonbConfig.failOnUnknownProperties)));

                // add generated serializers to config
                if (!typeToGeneratedSerializers.isEmpty()) {
                    ResultHandle serializersArray = instanceNull.newArray(JsonbSerializer.class,
                            instanceNull.load(typeToGeneratedSerializers.size()));
                    int i = 0;
                    for (Map.Entry<String, String> entry : typeToGeneratedSerializers.entrySet()) {

                        ResultHandle serializer = instanceNull
                                .newInstance(MethodDescriptor.ofConstructor(entry.getValue()));

                        // build up the serializers array that will be passed to JsonbConfig
                        instanceNull.writeArrayValue(serializersArray, instanceNull.load(i), serializer);

                        ResultHandle clazz = instanceNull.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Class.class, "forName", Class.class, String.class),
                                instanceNull.load(entry.getKey()));

                        // add a ContainerSerializerProvider for the serializer
                        ResultHandle serializerProvider = instanceNull.newInstance(
                                MethodDescriptor.ofConstructor(SimpleContainerSerializerProvider.class, JsonbSerializer.class),
                                serializer);
                        instanceNull.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(MappingContext.class, "addSerializerProvider", void.class,
                                        Class.class, ContainerSerializerProvider.class),
                                mappingContext, clazz, serializerProvider);

                        i++;
                    }
                    instanceNull.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(jsonbConfigClass, "withSerializers", jsonbConfigClass,
                                    JsonbSerializer[].class),
                            config, serializersArray);
                }

                // create jsonb from QuarkusJsonbBinding
                ResultHandle jsonb = instanceNull.newInstance(
                        MethodDescriptor.ofConstructor(QuarkusJsonbBinding.class, JsonbContext.class), jsonbContext);

                instanceNull.writeStaticField(instance, jsonb);
                instanceNull.returnValue(jsonb);
            }

            try (MethodCreator bridgeGetContext = cc.getMethodCreator("getContext", Object.class, Class.class)) {
                MethodDescriptor getContext = MethodDescriptor.ofMethod(QUARKUS_CONTEXT_RESOLVER, "getContext",
                        "javax.json.bind.Jsonb",
                        "java.lang.Class");
                ResultHandle result = bridgeGetContext.invokeVirtualMethod(getContext, bridgeGetContext.getThis(),
                        bridgeGetContext.getMethodParam(0));
                bridgeGetContext.returnValue(result);
                bridgeGetContext.returnValue(bridgeGetContext.readStaticField(instance));
            }
        }
    }
}
