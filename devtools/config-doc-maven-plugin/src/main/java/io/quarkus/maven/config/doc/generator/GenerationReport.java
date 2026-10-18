package io.quarkus.maven.config.doc.generator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.annotation.processor.documentation.config.model.SourceElementType;

public class GenerationReport {

    private Map<String, List<GenerationViolation>> violations = new LinkedHashMap<>();

    void addError(GenerationViolation error) {
        this.violations.computeIfAbsent(error.sourceType(), k -> new ArrayList<>()).add(error);
    }

    public Map<String, List<GenerationViolation>> getViolations() {
        return violations;
    }

    public record ConfigPropertyGenerationViolation(String sourceType, String sourceElement,
            SourceElementType sourceElementType, String message) implements GenerationViolation {

        @Override
        public String sourceElement() {
            return sourceElement + (sourceElementType == SourceElementType.METHOD ? "()" : "");
        }
    }

    public interface GenerationViolation {

        String sourceType();

        String sourceElement();

        String message();
    }
}
