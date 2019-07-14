package io.quarkus.resteasy.jsonb.deployment.serializers;

import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

public interface TypeSerializerGenerator {

    boolean supports(Type type, TypeSerializerGeneratorRegistry registry);

    void generate(GenerateContext context);

    class GenerateContext {
        private final Type type;
        private final BytecodeCreator bytecodeCreator;
        private final ResultHandle jsonGenerator;
        private final ResultHandle currentItem;
        private final TypeSerializerGeneratorRegistry registry;
        private final GlobalSerializationConfig globalConfig;

        // only used when the context is built for a property
        private final boolean nullChecked;
        private final Map<DotName, AnnotationInstance> effectivePropertyAnnotations;

        public GenerateContext(Type type, BytecodeCreator bytecodeCreator, ResultHandle jsonGenerator,
                ResultHandle currentItem,
                TypeSerializerGeneratorRegistry registry, GlobalSerializationConfig globalConfig,
                boolean nullChecked, Map<DotName, AnnotationInstance> effectivePropertyAnnotations) {
            this.type = type;
            this.bytecodeCreator = bytecodeCreator;
            this.jsonGenerator = jsonGenerator;
            this.currentItem = currentItem;
            this.registry = registry;
            this.globalConfig = globalConfig;
            this.nullChecked = nullChecked;
            this.effectivePropertyAnnotations = effectivePropertyAnnotations;
        }

        Type getType() {
            return type;
        }

        BytecodeCreator getBytecodeCreator() {
            return bytecodeCreator;
        }

        ResultHandle getJsonGenerator() {
            return jsonGenerator;
        }

        ResultHandle getCurrentItem() {
            return currentItem;
        }

        TypeSerializerGeneratorRegistry getRegistry() {
            return registry;
        }

        GlobalSerializationConfig getGlobalConfig() {
            return globalConfig;
        }

        boolean isNullChecked() {
            return nullChecked;
        }

        Map<DotName, AnnotationInstance> getEffectivePropertyAnnotations() {
            return effectivePropertyAnnotations;
        }

        GenerateContext changeItem(BytecodeCreator newBytecodeCreator, Type newType, ResultHandle newCurrentItem,
                boolean newNullChecked) {
            return new GenerateContext(newType, newBytecodeCreator, jsonGenerator, newCurrentItem, registry,
                    globalConfig,
                    newNullChecked, effectivePropertyAnnotations);
        }

        GenerateContext changeItem(BytecodeCreator newBytecodeCreator, Type newType,
                ResultHandle newCurrentItem, boolean newNullChecked,
                Map<DotName, AnnotationInstance> newEffectivePropertyAnnotations) {
            return new GenerateContext(newType, newBytecodeCreator, jsonGenerator, newCurrentItem, registry,
                    globalConfig, newNullChecked, newEffectivePropertyAnnotations);
        }

        GenerateContext changeByteCodeCreator(BytecodeCreator newBytecodeCreator) {
            return new GenerateContext(type, newBytecodeCreator, jsonGenerator, currentItem, registry,
                    globalConfig, nullChecked, effectivePropertyAnnotations);
        }
    }
}
