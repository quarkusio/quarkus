package io.quarkus.tika.deployment;

import java.util.Collections;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class TestConfigSource implements ConfigSource {

    @Override
    public Map<String, String> getProperties() {
        return Collections.singletonMap("opendoc", "org.apache.tika.parser.odf.OpenDocumentParser");
    }

    @Override
    public String getValue(String propertyName) {
        return getProperties().get(propertyName);
    }

    @Override
    public String getName() {
        return "test-source";
    }
}
