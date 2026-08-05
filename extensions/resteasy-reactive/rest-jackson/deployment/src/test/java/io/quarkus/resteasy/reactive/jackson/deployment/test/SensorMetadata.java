package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The metadata associated with a power-consumption recording sensor. This allows to make sense of the data sent by the power
 * server by providing information about each component (e.g. CPU) recorded by the sensor during each periodical measure.
 */
public class SensorMetadata {
    @JsonProperty("metadata")
    private final Map<String, ComponentMetadata> components;
    @JsonProperty("documentation")
    private final String documentation;

    /**
     * Initializes sensor metadata information
     *
     * @param components a map describing the metadata for each component
     * @param documentation a text providing any relevant information associated with the described sensor
     * @throws IllegalArgumentException if indices specified in {@code totalComponents} do not represent power measures
     *         expressible in Watts or are not a valid index
     */
    public SensorMetadata(List<ComponentMetadata> components, String documentation) {
        Objects.requireNonNull(components, "Must provide components");
        if (components.isEmpty()) {
            throw new IllegalArgumentException("Must provide at least one component");
        }

        final var cardinality = components.size();
        this.components = new LinkedHashMap<>(cardinality);
        this.documentation = documentation;
        final var indices = new BitSet(cardinality);
        components.stream().sorted(Comparator.comparingInt(ComponentMetadata::index)).forEach(component -> {
            // check that index is valid
            final var index = component.index;
            // record index as known
            indices.set(index);
            this.components.put(component.name, component);
        });
    }

    @JsonCreator
    SensorMetadata(@JsonProperty("metadata") Map<String, ComponentMetadata> components,
            @JsonProperty("documentation") String documentation) {
        this.components = components;
        this.documentation = documentation;
    }

    public static Builder withNewComponent(String name, String description, boolean isAttributed,
            String unitSymbol) {
        return new Builder().withNewComponent(name, description, isAttributed, unitSymbol);
    }

    public static Builder withNewComponent(String name, String description, boolean isAttributed,
            SensorUnit unit) {
        return new Builder().withNewComponent(name, description, isAttributed, unit);
    }

    public static Builder from(SensorMetadata sensorMetadata) {
        final var builder = new Builder();
        sensorMetadata.components.values().stream().sorted(Comparator.comparing(ComponentMetadata::index))
                .forEach(component -> builder.withNewComponent(component.name, component.description, component.isAttributed,
                        component.unit));
        return builder;
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder();
        components.values().stream().sorted(Comparator.comparing(ComponentMetadata::index))
                .forEach(cm -> sb.append("- ").append(cm).append("\n"));
        return "components:\n"
                + sb
                + (documentation != null ? "documentation: " + documentation : "");
    }

    /**
     * Determines whether a component with the specified name is known for this sensor
     *
     * @param component the name of the component
     * @return {@code true} if a component with the specified name exists for the associated sensor, {@code false} otherwise
     */
    public boolean exists(String component) {
        return components.containsKey(component);
    }

    /**
     * Retrieves the {@link ComponentMetadata} associated with the specified component name if it exists
     *
     * @param component the name of the component which metadata is to be retrieved
     * @return the {@link ComponentMetadata} associated with the specified name
     * @throws IllegalArgumentException if no component with the specified name is known for the associated sensor
     */
    public ComponentMetadata metadataFor(String component) throws IllegalArgumentException {
        final var componentMetadata = components.get(component);
        if (componentMetadata == null) {
            throw new IllegalArgumentException("Unknown component: " + component);
        }
        return componentMetadata;
    }

    /**
     * Retrieves the number of known components for the associated sensor
     *
     * @return the cardinality of known components
     */
    public int componentCardinality() {
        return components.size();
    }

    /**
     * Retrieves the known {@link ComponentMetadata} for the associated sensor as an unmodifiable Map keyed by the components'
     * name
     *
     * @return an unmodifiable Map of the known {@link ComponentMetadata}
     */
    public Map<String, ComponentMetadata> components() {
        return components;
    }

    /**
     * Retrieves the documentation, if any, associated with this SensorMetadata
     *
     * @return the documentation relevant for the associated sensor
     */
    public String documentation() {
        return documentation;
    }

    /**
     * Retrieves the metadata associated with the specified component index if it exists.
     *
     * @param componentIndex the index of the component we want to retrieve the metadata for
     * @return the {@link ComponentMetadata} associated with the specified index if it exists
     * @throws IllegalArgumentException if no component is associated with the specified index
     */
    public ComponentMetadata metadataFor(int componentIndex) {
        return components.values().stream()
                .filter(cm -> componentIndex == cm.index)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No component was found for index " + componentIndex));
    }

    @SuppressWarnings("unused")
    public static class Builder {
        private final List<ComponentMetadata> components = new ArrayList<>();
        private int currentIndex = 0;
        private String documentation;

        public Builder withNewComponent(String name, String description, boolean isAttributed, String unitSymbol) {
            components
                    .add(new ComponentMetadata(name, currentIndex++, description, isAttributed, unitSymbol));
            return this;
        }

        public Builder withNewComponent(String name, String description, boolean isAttributed, SensorUnit unit) {
            components.add(new ComponentMetadata(name, currentIndex++, description, isAttributed, unit));
            return this;
        }

        public Builder withDocumentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public SensorMetadata build() {
            return new SensorMetadata(components, documentation);
        }

        public Builder withNewComponent(ComponentMetadata metadata) {
            if (-1 == metadata.index) {
                metadata = new ComponentMetadata(metadata.name, currentIndex++, metadata.description,
                        metadata.isAttributed, metadata.unit);
            }
            components.add(metadata);
            return this;
        }
    }

    public record ComponentMetadata(String name, int index, String description, boolean isAttributed, SensorUnit unit) {

        public ComponentMetadata {
            if (name == null) {
                throw new IllegalArgumentException("Component name cannot be null");
            }
            if (unit == null) {
                throw new IllegalArgumentException("Component unit cannot be null");
            }
        }

        /**
         * Creates a ComponentMetadata that will be automatically assigned an index whenever added to a {@link SensorMetadata},
         * based on the contextual order of how other components have been added.
         */
        public ComponentMetadata(String name, String description, boolean isAttributed, SensorUnit unit) {
            this(name, -1, description, isAttributed, unit);
        }

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public ComponentMetadata(@JsonProperty("name") String name, @JsonProperty("index") int index,
                @JsonProperty("description") String description,
                @JsonProperty("isAttributed") boolean isAttributed, @JsonProperty("unit") String unitSymbol) {
            this(name, index, description, isAttributed, SensorUnit.of(unitSymbol));
        }

        @JsonProperty("unit")
        public String unitAsSymbol() {
            return unit.symbol();
        }
    }
}
