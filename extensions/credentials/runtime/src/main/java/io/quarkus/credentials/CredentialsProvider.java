package io.quarkus.credentials;

import java.util.Map;

public interface CredentialsProvider {

    String USER_PROPERTY_NAME = "user";
    String PASSWORD_PROPERTY_NAME = "password";

    Map<String, String> getCredentials(String credentialsProviderName);

}
