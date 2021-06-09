package io.quarkus.vault.runtime.client.dto.transit;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitReadKeyData implements VaultModel {

    public String detail;
    @JsonProperty(value = "deletion_allowed")
    public boolean deletionAllowed;
    public boolean derived;
    public boolean exportable;
    @JsonProperty(value = "allow_plaintext_backup")
    public boolean allowPlaintextBackup;
    @JsonDeserialize(contentUsing = VaultTransitKeyVersionData.Deserializer.class)
    public Map<String, VaultTransitKeyVersionData> keys;
    @JsonProperty(value = "latest_version")
    public int latestVersion;
    @JsonProperty(value = "min_available_version")
    public int minAvailableVersion;
    @JsonProperty(value = "min_decryption_version")
    public int minDecryptionVersion;
    @JsonProperty(value = "min_encryption_version")
    public int minEncryptionVersion;
    public String name;
    @JsonProperty(value = "supports_encryption")
    public boolean supportsEncryption;
    @JsonProperty(value = "supports_decryption")
    public boolean supportsDecryption;
    @JsonProperty(value = "supports_derivation")
    public boolean supportsDerivation;
    @JsonProperty(value = "supports_signing")
    public boolean supportsSigning;
    public String type;
}
