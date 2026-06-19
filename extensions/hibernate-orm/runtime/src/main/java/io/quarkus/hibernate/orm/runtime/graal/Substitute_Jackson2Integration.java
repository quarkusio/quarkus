package io.quarkus.hibernate.orm.runtime.graal;

import java.util.Collections;
import java.util.List;

import org.hibernate.type.format.FormatMapperCreationContext;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Substitution for {@code JacksonIntegration} to remove any Jackson 2 handling
 */
@TargetClass(className = "org.hibernate.type.format.jackson.JacksonIntegration")
final class Substitute_Jackson2Integration {

    @Substitute
    private static boolean ableToLoadJacksonXMLMapper() {
        return false;
    }

    @Substitute
    private static boolean ableToLoadJacksonJSONMapper() {
        return false;
    }

    @Substitute
    private static boolean ableToLoadJacksonOSONFactory() {
        return false;
    }

    @Substitute
    @SuppressWarnings("rawtypes")
    static List loadModules(FormatMapperCreationContext context) {
        return Collections.emptyList();
    }
}
