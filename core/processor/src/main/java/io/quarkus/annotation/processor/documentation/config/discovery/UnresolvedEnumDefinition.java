package io.quarkus.annotation.processor.documentation.config.discovery;

import java.util.Map;

/**
 * This is an unresolved enum. It might get resolved at some point on final assembly.
 */
public record UnresolvedEnumDefinition(String qualifiedName, Map<String, UnresolvedEnumConstant> constants) {

    public record UnresolvedEnumConstant(String explicitValue) {

        public boolean hasExplicitValue() {
            return explicitValue != null;
        }
    }
}
