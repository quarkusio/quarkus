package io.quarkus.agroal.runtime;

import java.util.Map;
import java.util.Properties;

import io.agroal.api.security.SimplePassword;
import io.quarkus.credentials.CredentialsProvider;

public class AgroalVaultCredentialsProviderPassword extends SimplePassword {

    private CredentialsProvider credentialsProvider;

    public AgroalVaultCredentialsProviderPassword(String credentialsProviderName, CredentialsProvider credentialsProvider) {
        super(credentialsProviderName);
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public Properties asProperties() {
        Properties properties = new Properties();
        Map<String, String> credentials = credentialsProvider.getCredentials(getWord());
        credentials.entrySet().forEach(entry -> properties.setProperty(entry.getKey(), entry.getValue()));
        return properties;
    }
}
