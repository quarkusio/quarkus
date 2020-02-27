package io.quarkus.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jboss.shrinkwrap.api.asset.Asset;

class PropertiesAsset implements Asset {
    private final Properties props;

    public PropertiesAsset(final Properties props) {
        this.props = props;
    }

    @Override
    public InputStream openStream() {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(128);
        try {
            props.store(outputStream, "Unit test Generated Application properties");
        } catch (IOException e) {
            throw new RuntimeException("Could not write application properties resource", e);
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}
