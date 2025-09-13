package io.quarkus.credentials;

import java.util.Map;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * Provides an indirection between credentials consumers such as Agroal and implementers such as Vault.
 * <p>
 * While not mandatory, it is recommended to implement the asynchronous {@link #getCredentialsAsync(String)} rather than
 * {@link #getCredentials(String)}.
 * Indeed, the synchronous variant may be removed in a future version.
 */
public interface CredentialsProvider {

    String USER_PROPERTY_NAME = "user";
    String PASSWORD_PROPERTY_NAME = "password";
    String EXPIRATION_TIMESTAMP_PROPERTY_NAME = "expires-at";

    /**
     * Returns the credentials for a given credentials provider.
     *
     * @param credentialsProviderName the name of the credentials provider, which can be used to retrieve custom configuration
     * @return the credentials
     * @deprecated invoke {@link #getCredentialsAsync(String)} instead
     */
    @Deprecated
    default Map<String, String> getCredentials(String credentialsProviderName) {
        throw new UnsupportedOperationException("Either `getCredentials` or `getCredentialsAsync` must be implemented`");
    }

    /**
     * Returns the credentials for a given credentials provider.
     *
     * @param credentialsProviderName the name of the credentials provider, which can be used to retrieve custom configuration
     * @return a {@link Uni} completed with the credentials, or failed
     */
    default Uni<Map<String, String>> getCredentialsAsync(String credentialsProviderName) {
        return Uni.createFrom().item(() -> getCredentials(credentialsProviderName))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }
}
