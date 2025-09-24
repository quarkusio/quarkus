package io.quarkus.credentials;

import java.util.Map;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * Provides an indirection between credentials consumers such as Agroal and implementers such as Vault.
 * <p>
 * Quarkus extensions <strong>MUST</strong> invoke the asynchronous variant. that is {@link #getCredentialsAsync(String)}.
 * <p>
 * The default implementation of asynchronous variant invokes the synchronous {@link #getCredentials(String)} on a worker
 * thread.
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
     */
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
