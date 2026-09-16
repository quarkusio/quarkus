package io.quarkus.avro.spi;

import java.util.Collection;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Marks classes as trusted for Avro deserialization.
 * <p>
 * Since Avro 1.12.2, classes are validated against a global security validator before they are deserialized; untrusted
 * classes cause a {@code java.lang.SecurityException}. Extensions that generate or handle classes which are deserialized
 * through Avro (for example Avro-generated records, or Protobuf-generated messages used with Pulsar) should produce this
 * build item so those classes pass Avro's security validation.
 * <p>
 * The {@code quarkus-avro} extension collects all instances of this build item and configures the Avro validator
 * accordingly. Producing it has no effect if the {@code quarkus-avro} extension is not present.
 */
public final class AvroTrustedClassBuildItem extends MultiBuildItem {

    private final Set<String> classNames;

    /**
     * @param className the fully qualified name of a class to trust
     */
    public AvroTrustedClassBuildItem(String className) {
        this.classNames = Set.of(className);
    }

    /**
     * @param classNames the fully qualified names of the classes to trust
     */
    public AvroTrustedClassBuildItem(Collection<String> classNames) {
        this.classNames = Set.copyOf(classNames);
    }

    public Set<String> getClassNames() {
        return classNames;
    }
}
