package io.quarkus.resteasy.jsonb.deployment.serializers;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import javax.json.stream.JsonGenerator;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.jsonb.deployment.DotNames;

public abstract class AbstractNumberTypeSerializerGenerator extends AbstractTypeSerializerGenerator {

    protected abstract void generateUnformatted(GenerateContext context);

    @Override
    public void generateNotNull(GenerateContext context) {
        if (context.getEffectivePropertyAnnotations().containsKey(DotNames.JSONB_NUMBER_FORMAT)) {
            AnnotationInstance jsonbNumberFormatInstance = context.getEffectivePropertyAnnotations()
                    .get(DotNames.JSONB_NUMBER_FORMAT);

            String format = ""; // the default value of the @JsonbTransient annotation
            AnnotationValue formatValue = jsonbNumberFormatInstance.value();
            if (formatValue != null) {
                format = formatValue.asString();
            }

            String locale = null;
            AnnotationValue localeValue = jsonbNumberFormatInstance.value("locale");
            if (localeValue != null) {
                locale = localeValue.asString();
            }

            BytecodeCreator bytecodeCreator = context.getBytecodeCreator();

            ResultHandle localeHandle = SerializerGeneratorUtil.getLocaleHandle(locale, bytecodeCreator);
            ResultHandle numberFormatHandle = bytecodeCreator
                    .invokeStaticMethod(
                            MethodDescriptor.ofMethod(NumberFormat.class, "getInstance", NumberFormat.class, Locale.class),
                            localeHandle);
            ResultHandle decimalNumberFormatHandle = bytecodeCreator.checkCast(numberFormatHandle, DecimalFormat.class);
            bytecodeCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(DecimalFormat.class, "applyPattern", void.class, String.class),
                    decimalNumberFormatHandle, bytecodeCreator.load(format));
            ResultHandle formattedValue = bytecodeCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(DecimalFormat.class, "format", String.class, Object.class),
                    decimalNumberFormatHandle, context.getCurrentItem());

            bytecodeCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(JsonGenerator.class, "write", JsonGenerator.class, String.class),
                    context.getJsonGenerator(),
                    formattedValue);
        } else {
            generateUnformatted(context);
        }
    }
}
