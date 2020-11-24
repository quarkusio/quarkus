package io.quarkus.vault;

import java.util.Map;

public class VaultTransitKeyDetail {

    private String name;
    private String detail;
    private boolean deletionAllowed;
    private boolean derived;
    private boolean exportable;
    private boolean allowPlaintextBackup;
    private Map<String, Integer> keys;
    private int minDecryptionVersion;
    private int minEncryptionVersion;
    private boolean supportsEncryption;
    private boolean supportsDecryption;
    private boolean supportsDerivation;
    private boolean supportsSigning;

    public String getName() {
        return name;
    }

    public VaultTransitKeyDetail setName(String name) {
        this.name = name;
        return this;
    }

    public String getDetail() {
        return detail;
    }

    public VaultTransitKeyDetail setDetail(String detail) {
        this.detail = detail;
        return this;
    }

    public boolean isDeletionAllowed() {
        return deletionAllowed;
    }

    public VaultTransitKeyDetail setDeletionAllowed(boolean deletionAllowed) {
        this.deletionAllowed = deletionAllowed;
        return this;
    }

    public boolean isDerived() {
        return derived;
    }

    public VaultTransitKeyDetail setDerived(boolean derived) {
        this.derived = derived;
        return this;
    }

    public boolean isExportable() {
        return exportable;
    }

    public VaultTransitKeyDetail setExportable(boolean exportable) {
        this.exportable = exportable;
        return this;
    }

    public boolean isAllowPlaintextBackup() {
        return allowPlaintextBackup;
    }

    public VaultTransitKeyDetail setAllowPlaintextBackup(boolean allowPlaintextBackup) {
        this.allowPlaintextBackup = allowPlaintextBackup;
        return this;
    }

    public Map<String, Integer> getKeys() {
        return keys;
    }

    public VaultTransitKeyDetail setKeys(Map<String, Integer> keys) {
        this.keys = keys;
        return this;
    }

    public int getMinDecryptionVersion() {
        return minDecryptionVersion;
    }

    public VaultTransitKeyDetail setMinDecryptionVersion(int minDecryptionVersion) {
        this.minDecryptionVersion = minDecryptionVersion;
        return this;
    }

    public int getMinEncryptionVersion() {
        return minEncryptionVersion;
    }

    public VaultTransitKeyDetail setMinEncryptionVersion(int minEncryptionVersion) {
        this.minEncryptionVersion = minEncryptionVersion;
        return this;
    }

    public boolean isSupportsEncryption() {
        return supportsEncryption;
    }

    public VaultTransitKeyDetail setSupportsEncryption(boolean supportsEncryption) {
        this.supportsEncryption = supportsEncryption;
        return this;
    }

    public boolean isSupportsDecryption() {
        return supportsDecryption;
    }

    public VaultTransitKeyDetail setSupportsDecryption(boolean supportsDecryption) {
        this.supportsDecryption = supportsDecryption;
        return this;
    }

    public boolean isSupportsDerivation() {
        return supportsDerivation;
    }

    public VaultTransitKeyDetail setSupportsDerivation(boolean supportsDerivation) {
        this.supportsDerivation = supportsDerivation;
        return this;
    }

    public boolean isSupportsSigning() {
        return supportsSigning;
    }

    public VaultTransitKeyDetail setSupportsSigning(boolean supportsSigning) {
        this.supportsSigning = supportsSigning;
        return this;
    }
}
