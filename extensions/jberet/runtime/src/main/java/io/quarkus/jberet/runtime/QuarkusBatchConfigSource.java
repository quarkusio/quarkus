package io.quarkus.jberet.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jberet.creation.ArtifactCreationContext;
import org.jberet.job.model.Properties;

public class QuarkusBatchConfigSource implements ConfigSource {
    @Override
    public Map<String, String> getProperties() {
        // Ideally, we would return all batch properties here, only names, to pass Quarkus validation and don't
        // require to set a default value. Or don't validate if the Config is also annotated with @BatchProperty.
        return Collections.emptyMap();
    }

    @Override
    public String getValue(final String propertyName) {
        Properties properties = ArtifactCreationContext.getCurrentArtifactCreationContext().getProperties();
        return Optional.ofNullable(properties).map(props -> props.get(propertyName)).orElse(null);
    }

    @Override
    public String getName() {
        return QuarkusBatchConfigSource.class.getName();
    }

    @Override
    public int getOrdinal() {
        return ConfigSource.DEFAULT_ORDINAL + 50;
    }
}
