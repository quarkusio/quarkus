package io.quarkus.hibernate.orm.runtime.graal;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.FormatMapperCreationContext;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Substitution for {@code JacksonIntegration} when Jackson is not on the classpath.
 */
@TargetClass(className = "org.hibernate.type.format.jackson.JacksonIntegration", onlyWith = Substitute_Jackson3Integration.IsJackson3Absent.class)
final class Substitute_Jackson3Integration {

    static final class IsJackson3Absent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("tools.jackson.databind.ObjectMapper");
                return false;
            } catch (ClassNotFoundException e) {
                return true;
            }
        }
    }

    @Substitute
    public static FormatMapper getXMLJackson3FormatMapperOrNull(FormatMapperCreationContext context) {
        return null;
    }

    @Substitute
    public static FormatMapper getJsonJackson3FormatMapperOrNull(FormatMapperCreationContext context) {
        return null;
    }

    @Substitute
    public static FormatMapper getJsonJackson3FormatMapperOrNull() {
        return null;
    }

    @Substitute
    private static boolean canLoad(String className) {
        return false;
    }

    @Substitute
    @SuppressWarnings("rawtypes")
    static List loadJackson3Modules(FormatMapperCreationContext context) {
        return Collections.emptyList();
    }
}
