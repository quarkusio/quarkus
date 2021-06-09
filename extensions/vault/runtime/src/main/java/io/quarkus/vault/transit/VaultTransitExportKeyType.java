package io.quarkus.vault.transit;

/**
 * key type used in transit key export
 */
public enum VaultTransitExportKeyType {
    encryption,
    signing,
    hmac
}
