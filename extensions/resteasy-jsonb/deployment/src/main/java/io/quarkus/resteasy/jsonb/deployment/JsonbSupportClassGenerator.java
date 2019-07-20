package io.quarkus.resteasy.jsonb.deployment;

import java.lang.reflect.Modifier;
import java.util.Locale;

import javax.json.bind.annotation.JsonbDateFormat;

import org.eclipse.yasson.internal.serializer.JsonbDateFormatter;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class JsonbSupportClassGenerator {

    public static final String QUARKUS_DEFAULT_DATE_FORMATTER_PROVIDER = "io.quarkus.jsonb.QuarkusDefaultJsonbDateFormatterProvider";
    public static final String QUARKUS_DEFAULT_LOCALE_PROVIDER = "io.quarkus.jsonb.QuarkusDefaultJsonbLocaleProvider";

    private final JsonbConfig jsonbConfig;

    public JsonbSupportClassGenerator(JsonbConfig jsonbConfig) {
        this.jsonbConfig = jsonbConfig;
    }

    public void generateDefaultLocaleProvider(ClassOutput classOutput) {
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

    public void generateJsonbDefaultJsonbDateFormatterProvider(ClassOutput classOutput) {
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
}
