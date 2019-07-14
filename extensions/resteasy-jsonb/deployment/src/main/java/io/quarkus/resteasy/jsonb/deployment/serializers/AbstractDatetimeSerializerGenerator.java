package io.quarkus.resteasy.jsonb.deployment.serializers;

import javax.json.bind.annotation.JsonbDateFormat;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import io.quarkus.resteasy.jsonb.deployment.DotNames;

public abstract class AbstractDatetimeSerializerGenerator extends AbstractTypeSerializerGenerator {

    protected abstract void doGenerate(GenerateContext context, String format, String locale);

    @Override
    protected void generateNotNull(GenerateContext context) {
        String format = JsonbDateFormat.DEFAULT_FORMAT;
        String locale = null;
        if (context.getEffectivePropertyAnnotations().containsKey(DotNames.JSONB_DATE_FORMAT)) {
            AnnotationInstance jsonbDateFormatInstance = context.getEffectivePropertyAnnotations()
                    .get(DotNames.JSONB_DATE_FORMAT);
            AnnotationValue formatValue = jsonbDateFormatInstance.value();
            if (formatValue != null) {
                format = formatValue.asString();
            }

            AnnotationValue localeValue = jsonbDateFormatInstance.value("locale");
            if (localeValue != null) {
                locale = localeValue.asString();
            }
        }

        doGenerate(context, format, locale);
    }
}
