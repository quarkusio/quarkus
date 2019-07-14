package io.quarkus.resteasy.jsonb.deployment.serializers;

import java.util.Locale;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.jsonb.deployment.AdditionalClassGenerator;

final class SerializerGeneratorUtil {

    private SerializerGeneratorUtil() {
    }

    static ResultHandle getLocaleHandle(String locale, BytecodeCreator bytecodeCreator) {
        if (locale == null) {
            // just use the default locale
            return bytecodeCreator
                    .invokeStaticMethod(MethodDescriptor.ofMethod(AdditionalClassGenerator.QUARKUS_DEFAULT_LOCALE_PROVIDER,
                            "get", Locale.class));
        }

        return bytecodeCreator.invokeStaticMethod(
                MethodDescriptor.ofMethod(Locale.class, "forLanguageTag", Locale.class, String.class),
                bytecodeCreator.load(locale));
    }
}
