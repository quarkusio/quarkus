package org.jboss.resteasy.reactive.server.processor.generation.injection;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.server.processor.ServerEndpointIndexer;
import org.jboss.resteasy.reactive.server.processor.ServerIndexedParameter;
import org.jboss.resteasy.reactive.server.processor.scanning.ClassInjectorTransformer;
import org.jboss.resteasy.reactive.server.processor.scanning.InjectedClassConverterField;
import org.objectweb.asm.ClassVisitor;

public class TransformedFieldInjectionIndexerExtension implements ServerEndpointIndexer.FieldInjectionIndexerExtension {
    final BiConsumer<String, BiFunction<String, ClassVisitor, ClassVisitor>> transformations;
    final boolean requireCreateBeanParams;
    private final Consumer<InjectedClassConverterField> injectedClassConverterFieldConsumer;

    public TransformedFieldInjectionIndexerExtension(
            BiConsumer<String, BiFunction<String, ClassVisitor, ClassVisitor>> transformations,
            boolean requireCreateBeanParams,
            Consumer<InjectedClassConverterField> injectedClassConverterFieldConsumer) {
        this.transformations = transformations;
        this.requireCreateBeanParams = requireCreateBeanParams;
        this.injectedClassConverterFieldConsumer = injectedClassConverterFieldConsumer;
    }

    @Override
    public void handleFieldInjection(String currentTypeName, Map<FieldInfo, ServerIndexedParameter> fieldExtractors,
            boolean superTypeIsInjectable, IndexView indexView) {
        for (Map.Entry<FieldInfo, ServerIndexedParameter> entry : fieldExtractors.entrySet()) {
            if (entry.getValue().getConverter() != null) {
                injectedClassConverterFieldConsumer.accept(new InjectedClassConverterField(
                        ClassInjectorTransformer.INIT_CONVERTER_METHOD_NAME + entry.getKey().name(), currentTypeName));
            }
        }
        transformations.accept(currentTypeName, new ClassInjectorTransformer(fieldExtractors, superTypeIsInjectable,
                requireCreateBeanParams, indexView));
    }
}
