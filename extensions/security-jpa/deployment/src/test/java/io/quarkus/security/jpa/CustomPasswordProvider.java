package io.quarkus.security.jpa;

import jakarta.xml.bind.DatatypeConverter;

import org.wildfly.security.password.Password;
import org.wildfly.security.password.interfaces.SimpleDigestPassword;

public class CustomPasswordProvider implements PasswordProvider {
    @Override
    public Password getPassword(String pass) {
        byte[] digest = DatatypeConverter.parseHexBinary(pass);
        return SimpleDigestPassword.createRaw(SimpleDigestPassword.ALGORITHM_SIMPLE_DIGEST_SHA_256, digest);
    }
}
