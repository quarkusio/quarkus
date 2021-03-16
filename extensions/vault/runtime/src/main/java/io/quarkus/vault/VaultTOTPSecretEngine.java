package io.quarkus.vault;

import java.util.List;
import java.util.Optional;

import io.quarkus.vault.secrets.totp.CreateKeyParameters;
import io.quarkus.vault.secrets.totp.KeyConfiguration;
import io.quarkus.vault.secrets.totp.KeyDefinition;

/**
 * This service provides access to the TOTP secret engine.
 *
 * @see <a href="https://www.vaultproject.io/api/secret/totp/index.html">TOTP Secrets Engine </a>
 */
public interface VaultTOTPSecretEngine {

    /**
     * Creates or updates a key definition.
     * 
     * @param name of the key.
     * @param createKeyParameters required to create or update a key.
     * @return Barcode and/or URL of the created OTP key.
     */
    Optional<KeyDefinition> createKey(String name, CreateKeyParameters createKeyParameters);

    /**
     * Queries the key definition.
     * 
     * @param name of the key.
     * @return The key configuration.
     */
    KeyConfiguration readKey(String name);

    /**
     * Returns a list of available keys. Only the key names are returned, not any values.
     * 
     * @return List of available keys.
     */
    List<String> listKeys();

    /**
     * Deletes the key definition.
     * 
     * @param name of the key.
     */
    void deleteKey(String name);

    /**
     * Generates a new time-based one-time use password based on the named key.
     * 
     * @param name of the key.
     * @return The Code.
     */
    String generateCode(String name);

    /**
     * Validates a time-based one-time use password generated from the named key.
     * 
     * @param name of the key.
     * @param code to validate.
     * @return True if valid, false otherwise.
     */
    boolean validateCode(String name, String code);
}
