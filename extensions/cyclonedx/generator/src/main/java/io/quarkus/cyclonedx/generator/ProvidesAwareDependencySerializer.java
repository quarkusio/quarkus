package io.quarkus.cyclonedx.generator;

import java.io.IOException;

import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.DependencyList;
import org.cyclonedx.util.serializer.DependencySerializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

/**
 * Extends the stock cyclonedx-core-java {@link DependencySerializer} to emit the CycloneDX 1.6
 * {@code provides} relationship, which the stock serializer does not support.
 * <p>
 * Only the JSON path is customized. For each dependency node the serializer writes the usual
 * {@code dependsOn} array and, when the node is a {@link ProvidesDependency} with provided refs,
 * an additional {@code provides} array. To keep the output of plain dependencies byte-identical
 * to the stock serializer, a node's {@code dependsOn} array is always written for a plain
 * {@link Dependency} (even when empty), whereas for a {@link ProvidesDependency} the
 * {@code dependsOn} array is written only when it is non-empty.
 * <p>
 * XML serialization is delegated to the superclass, so {@code provides} refs are not represented in
 * XML for now (the XML path continues to use {@code dependsOn} only).
 */
public class ProvidesAwareDependencySerializer extends DependencySerializer {

    private static final String REF = "ref";
    private static final String DEPENDS_ON = "dependsOn";
    private static final String PROVIDES = "provides";

    private final boolean useNamespace;

    public ProvidesAwareDependencySerializer() {
        this(false, null);
    }

    public ProvidesAwareDependencySerializer(boolean useNamespace, String parentTagName) {
        super(useNamespace, parentTagName);
        this.useNamespace = useNamespace;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        // Preserve the superclass contract of carrying the property name as the XML parent tag name,
        // while ensuring the contextual serializer remains provides-aware.
        return new ProvidesAwareDependencySerializer(useNamespace, property.getName());
    }

    @Override
    public void serialize(DependencyList dependencies, JsonGenerator generator, SerializerProvider provider)
            throws IOException {
        // The XML path is not customized; the stock serializer emits every relationship as dependsOn.
        if (generator instanceof ToXmlGenerator) {
            super.serialize(dependencies, generator, provider);
            return;
        }
        if (dependencies == null) {
            return;
        }
        generator.writeStartArray();
        for (Dependency dependency : dependencies) {
            generator.writeStartObject();
            generator.writeStringField(REF, dependency.getRef());
            if (dependency instanceof ProvidesDependency) {
                writeProvidesDependency((ProvidesDependency) dependency, generator);
            } else {
                writeDependsOn(dependency, generator, true);
            }
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    private void writeProvidesDependency(ProvidesDependency dependency, JsonGenerator generator) throws IOException {
        // Only emit dependsOn when it actually carries refs, so a provides-only node stays uncluttered.
        boolean hasDependsOn = dependency.getDependencies() != null && !dependency.getDependencies().isEmpty();
        if (hasDependsOn) {
            writeDependsOn(dependency, generator, false);
        }
        generator.writeArrayFieldStart(PROVIDES);
        for (String ref : dependency.getProvides()) {
            generator.writeString(ref);
        }
        generator.writeEndArray();
    }

    private void writeDependsOn(Dependency dependency, JsonGenerator generator, boolean writeWhenEmpty)
            throws IOException {
        boolean hasDependsOn = dependency.getDependencies() != null && !dependency.getDependencies().isEmpty();
        if (!hasDependsOn && !writeWhenEmpty) {
            return;
        }
        generator.writeArrayFieldStart(DEPENDS_ON);
        if (hasDependsOn) {
            for (Dependency subDependency : dependency.getDependencies()) {
                generator.writeString(subDependency.getRef());
            }
        }
        generator.writeEndArray();
    }
}
