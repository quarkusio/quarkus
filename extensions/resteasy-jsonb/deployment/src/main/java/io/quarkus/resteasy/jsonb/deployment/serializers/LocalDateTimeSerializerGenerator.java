package io.quarkus.resteasy.jsonb.deployment.serializers;

import java.time.LocalDateTime;
import java.util.Locale;

import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.stream.JsonGenerator;

import org.jboss.jandex.Type;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.jsonb.deployment.DotNames;
import io.quarkus.resteasy.jsonb.runtime.serializers.LocalDateTimeSerializerHelper;

public class LocalDateTimeSerializerGenerator extends AbstractDatetimeSerializerGenerator {

    @Override
    public boolean supports(Type type, TypeSerializerGeneratorRegistry registry) {
        return DotNames.LOCAL_DATE_TIME.equals(type.name());
    }

    @Override
    protected void doGenerate(GenerateContext context, String format, String locale) {
        BytecodeCreator bytecodeCreator = context.getBytecodeCreator();

        ResultHandle localeHandle = SerializerGeneratorUtil.getLocaleHandle(locale, bytecodeCreator);
        ResultHandle stringValueHandle = getStringValueResultHandle(context, format, localeHandle);

        context.getBytecodeCreator().invokeInterfaceMethod(
                MethodDescriptor.ofMethod(JsonGenerator.class, "write", JsonGenerator.class, String.class),
                context.getJsonGenerator(),
                stringValueHandle);
    }

    private ResultHandle getStringValueResultHandle(GenerateContext context, String format, ResultHandle localeHandle) {
        BytecodeCreator bytecodeCreator = context.getBytecodeCreator();
        if (JsonbDateFormat.DEFAULT_FORMAT.equals(format)) {
            return bytecodeCreator.invokeStaticMethod(
                    MethodDescriptor.ofMethod(LocalDateTimeSerializerHelper.class, "defaultFormat", String.class,
                            LocalDateTime.class, Locale.class),
                    context.getCurrentItem(), localeHandle);
        } else if (JsonbDateFormat.TIME_IN_MILLIS.equals(format)) {
            return bytecodeCreator.invokeStaticMethod(
                    MethodDescriptor.ofMethod(LocalDateTimeSerializerHelper.class, "timeInMillisFormat", String.class,
                            LocalDateTime.class),
                    context.getCurrentItem());
        }

        return bytecodeCreator.invokeStaticMethod(
                MethodDescriptor.ofMethod(LocalDateTimeSerializerHelper.class, "customFormat", String.class,
                        LocalDateTime.class, String.class, Locale.class),
                context.getCurrentItem(), bytecodeCreator.load(format), localeHandle);
    }
}
