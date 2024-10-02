package io.quarkus.annotation.processor.documentation.config.discovery;

import java.util.Map;

/**
 * This is an uncontextual enum definition obtained from the enum type.
 * <p>
 * At resolution, the enum will get contextualized to how it's consumed in the config property (and for instance, might be
 * hyphenated if needed).
 */
public record EnumDefinition(String qualifiedName, Map<String, EnumConstant> constants) {

    public record EnumConstant(String explicitValue) {

        public boolean hasExplicitValue() {
            return explicitValue != null;
        }
    }
}
