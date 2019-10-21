package io.quarkus.agroal.runtime;

import java.util.Properties;

import io.agroal.api.security.SimplePassword;
import io.quarkus.vault.CredentialsProvider;

public class AgroalVaultCredentialsProviderPassword extends SimplePassword {

    private CredentialsProvider credentialsProvider;

    public AgroalVaultCredentialsProviderPassword(String credentialsProviderName, CredentialsProvider credentialsProvider) {
        super(credentialsProviderName);
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public Properties asProperties() {
        return credentialsProvider.getCredentials(getWord());
    }
}
