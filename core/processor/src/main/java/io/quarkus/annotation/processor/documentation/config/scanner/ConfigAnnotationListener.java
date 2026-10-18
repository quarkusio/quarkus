package io.quarkus.annotation.processor.documentation.config.scanner;

import java.util.Optional;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigGroup;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigRoot;
import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryRootElement;
import io.quarkus.annotation.processor.documentation.config.discovery.ResolvedType;

public interface ConfigAnnotationListener {

    default Optional<DiscoveryConfigRoot> onConfigRoot(TypeElement configRoot) {
        return Optional.empty();
    }

    default void onSuperclass(DiscoveryRootElement discoveryRootElement, TypeElement superClass) {
    }

    default void onInterface(DiscoveryRootElement discoveryRootElement, TypeElement interfaze) {
    }

    default Optional<DiscoveryConfigGroup> onConfigGroup(TypeElement configGroup) {
        return Optional.empty();
    }

    default void onEnclosedMethod(DiscoveryRootElement discoveryRootElement, TypeElement clazz, ExecutableElement method,
            ResolvedType type) {
    }

    default void onEnclosedField(DiscoveryRootElement discoveryRootElement, TypeElement clazz, VariableElement field,
            ResolvedType type) {
    }

    default void onResolvedEnum(TypeElement enumTypeElement) {
    }

    default void finalizeProcessing() {
    }
}
