package io.quarkus.vault;

/**
 * key type used in transit key export
 */
public enum VaultTransitExportKeyType {
    encryption,
    signing,
    hmac
}
