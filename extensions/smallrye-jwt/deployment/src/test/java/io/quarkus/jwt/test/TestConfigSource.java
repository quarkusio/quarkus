package io.quarkus.jwt.test;

import java.io.IOException;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.jwt.util.KeyUtils;

public class TestConfigSource implements ConfigSource {

    @Override
    public Set<String> getPropertyNames() {
        return Set.of("sign-key", "encrypt-key");
    }

    @Override
    public String getValue(String propertyName) {
        if ("sign-key".equals(propertyName)) {
            return readKeyContent("/privateKey.pem");
        } else if ("encrypt-key".equals(propertyName)) {
            return readKeyContent("/publicKey.pem");
        } else {
            return null;
        }
    }

    private String readKeyContent(String keyLocation) {
        try {
            return KeyUtils.readKeyContent(keyLocation);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String getName() {
        return "jwt";
    }

}
