package io.quarkus.vault.transit;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Map.Entry;

public abstract class VaultTransitKeyDetail<KEYVER extends VaultTransitKeyVersion> {

    private String name;
    private String detail;
    private boolean deletionAllowed;
    private boolean derived;
    private boolean exportable;
    private boolean allowPlaintextBackup;
    private Map<String, KEYVER> versions;
    private int latestVersion;
    private int minAvailableVersion;
    private int minDecryptionVersion;
    private int minEncryptionVersion;
    private boolean supportsEncryption;
    private boolean supportsDecryption;
    private boolean supportsDerivation;
    private boolean supportsSigning;
    private String type;

    public String getName() {
        return name;
    }

    public VaultTransitKeyDetail<KEYVER> setName(String name) {
        this.name = name;
        return this;
    }

    public String getDetail() {
        return detail;
    }

    public VaultTransitKeyDetail<KEYVER> setDetail(String detail) {
        this.detail = detail;
        return this;
    }

    public boolean isDeletionAllowed() {
        return deletionAllowed;
    }

    public VaultTransitKeyDetail<KEYVER> setDeletionAllowed(boolean deletionAllowed) {
        this.deletionAllowed = deletionAllowed;
        return this;
    }

    public boolean isDerived() {
        return derived;
    }

    public VaultTransitKeyDetail<KEYVER> setDerived(boolean derived) {
        this.derived = derived;
        return this;
    }

    public boolean isExportable() {
        return exportable;
    }

    public VaultTransitKeyDetail<KEYVER> setExportable(boolean exportable) {
        this.exportable = exportable;
        return this;
    }

    public boolean isAllowPlaintextBackup() {
        return allowPlaintextBackup;
    }

    public VaultTransitKeyDetail<KEYVER> setAllowPlaintextBackup(boolean allowPlaintextBackup) {
        this.allowPlaintextBackup = allowPlaintextBackup;
        return this;
    }

    /**
     * Returns a map of version numbers to Unix epoch timestamps of when the version was created.
     *
     * @deprecated This method has been deprecated in favor of {@link #getVersions()} which provides the same data
     *             via the {@link VaultTransitKeyVersion#getCreationTime()} property.
     */
    @Deprecated
    public Map<String, Integer> getKeys() {
        return versions.entrySet().stream()
                .collect(toMap(Entry::getKey, entry -> (int) entry.getValue().getCreationTime().toEpochSecond()));
    }

    /**
     * Returns a map of version numbers to {@link VaultTransitKeyVersion} objects.
     */
    public Map<String, KEYVER> getVersions() {
        return versions;
    }

    public VaultTransitKeyDetail<KEYVER> setVersions(Map<String, KEYVER> versions) {
        this.versions = versions;
        return this;
    }

    public int getLatestVersion() {
        return latestVersion;
    }

    public VaultTransitKeyDetail<KEYVER> setLatestVersion(int latestVersion) {
        this.latestVersion = latestVersion;
        return this;
    }

    public int getMinAvailableVersion() {
        return minAvailableVersion;
    }

    public VaultTransitKeyDetail<KEYVER> setMinAvailableVersion(int minAvailableVersion) {
        this.minAvailableVersion = minAvailableVersion;
        return this;
    }

    public int getMinDecryptionVersion() {
        return minDecryptionVersion;
    }

    public VaultTransitKeyDetail<KEYVER> setMinDecryptionVersion(int minDecryptionVersion) {
        this.minDecryptionVersion = minDecryptionVersion;
        return this;
    }

    public int getMinEncryptionVersion() {
        return minEncryptionVersion;
    }

    public VaultTransitKeyDetail<KEYVER> setMinEncryptionVersion(int minEncryptionVersion) {
        this.minEncryptionVersion = minEncryptionVersion;
        return this;
    }

    public boolean isSupportsEncryption() {
        return supportsEncryption;
    }

    public VaultTransitKeyDetail<KEYVER> setSupportsEncryption(boolean supportsEncryption) {
        this.supportsEncryption = supportsEncryption;
        return this;
    }

    public boolean isSupportsDecryption() {
        return supportsDecryption;
    }

    public VaultTransitKeyDetail<KEYVER> setSupportsDecryption(boolean supportsDecryption) {
        this.supportsDecryption = supportsDecryption;
        return this;
    }

    public boolean isSupportsDerivation() {
        return supportsDerivation;
    }

    public VaultTransitKeyDetail<KEYVER> setSupportsDerivation(boolean supportsDerivation) {
        this.supportsDerivation = supportsDerivation;
        return this;
    }

    public boolean isSupportsSigning() {
        return supportsSigning;
    }

    public VaultTransitKeyDetail<KEYVER> setSupportsSigning(boolean supportsSigning) {
        this.supportsSigning = supportsSigning;
        return this;
    }

    public String getType() {
        return type;
    }

    public VaultTransitKeyDetail<KEYVER> setType(String type) {
        this.type = type;
        return this;
    }
}
