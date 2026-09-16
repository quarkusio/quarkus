package io.quarkus.cyclonedx.generator;

import org.cyclonedx.Version;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * A {@link BomJsonGenerator} that understands {@link ProvidesDependency} nodes and serializes their
 * refs under the CycloneDX 1.6 {@code provides} relationship, which the stock generator does not
 * support.
 * <p>
 * The stock generator wires a {@link org.cyclonedx.util.serializer.DependencySerializer} onto its
 * (inherited, {@code protected}) {@link #mapper ObjectMapper} during construction. This subclass
 * registers a {@link ProvidesAwareDependencySerializer} afterwards: Jackson gives precedence to the
 * most recently registered serializer for a given type, so ours supersedes the stock one for
 * {@link org.cyclonedx.model.DependencyList}. All other cyclonedx serialization behavior and
 * formatting is inherited unchanged.
 */
public class ProvidesAwareBomJsonGenerator extends BomJsonGenerator {

    public ProvidesAwareBomJsonGenerator(Bom bom, Version version) {
        super(bom, version);
        SimpleModule module = new SimpleModule();
        module.addSerializer(new ProvidesAwareDependencySerializer());
        mapper.registerModule(module);
    }
}
