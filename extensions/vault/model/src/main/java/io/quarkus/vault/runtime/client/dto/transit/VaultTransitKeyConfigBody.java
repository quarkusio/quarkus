package io.quarkus.vault.runtime.client.dto.transit;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitKeyConfigBody implements VaultModel {

    @JsonProperty("min_decryption_version")
    public Integer minDecryptionVersion;
    @JsonProperty("min_encryption_version")
    public Integer minEncryptionVersion;
    @JsonProperty("deletion_allowed")
    public Boolean deletionAllowed;
    public Boolean exportable;
    @JsonProperty("allow_plaintext_backup")
    public Boolean allowPlaintextBackup;
}
