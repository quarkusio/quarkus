package org.jboss.shamrock.transactions.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.arjuna.common.util.propertyservice.AbstractPropertiesFactory;

public class ShamrockPropertiesFactory extends AbstractPropertiesFactory {

    private final Properties properties;

    public ShamrockPropertiesFactory(Properties properties) {
        this.properties = properties;
    }

    @Override
    protected Properties loadFromXML(Properties p, InputStream is) throws IOException {
        return properties;
    }
}
