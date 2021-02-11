package io.quarkus.narayana.jta.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.arjuna.common.util.propertyservice.AbstractPropertiesFactory;

public class QuarkusPropertiesFactory extends AbstractPropertiesFactory {

    private final Properties properties;

    public QuarkusPropertiesFactory(Properties properties) {
        this.properties = properties;
    }

    @Override
    protected Properties loadFromXML(Properties p, InputStream is) throws IOException {
        return properties;
    }
}
