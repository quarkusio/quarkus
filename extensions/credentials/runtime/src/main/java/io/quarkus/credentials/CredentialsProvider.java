package io.quarkus.credentials;

import java.util.Map;

/**
 * Provides an indirection between credentials consumers such as Agroal and implementers such as Vault.
 */
public interface CredentialsProvider {

    String USER_PROPERTY_NAME = "user";
    String PASSWORD_PROPERTY_NAME = "password";

    /**
     * Returns the credentials for a given credentials provider
     * 
     * @param credentialsProviderName the name of the credentials provider, which can be used to retrieve custom configuration
     * @return the credentials
     */
    Map<String, String> getCredentials(String credentialsProviderName);

}
