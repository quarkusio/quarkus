package io.quarkus.vault;

import java.util.Properties;

public interface CredentialsProvider {

    String USER_PROPERTY_NAME = "user";
    String PASSWORD_PROPERTY_NAME = "password";

    Properties getCredentials(String credentialsProviderName);

}
