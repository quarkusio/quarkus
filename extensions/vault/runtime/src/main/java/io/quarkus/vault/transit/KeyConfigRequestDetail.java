package io.quarkus.vault.transit;

public class KeyConfigRequestDetail {

    public Integer minDecryptionVersion;
    public Integer minEncryptionVersion;
    public Boolean deletionAllowed;
    public Boolean exportable;
    public Boolean allowPlaintextBackup;

    public Integer getMinDecryptionVersion() {
        return minDecryptionVersion;
    }

    public KeyConfigRequestDetail setMinDecryptionVersion(Integer minDecryptionVersion) {
        this.minDecryptionVersion = minDecryptionVersion;
        return this;
    }

    public Integer getMinEncryptionVersion() {
        return minEncryptionVersion;
    }

    public KeyConfigRequestDetail setMinEncryptionVersion(Integer minEncryptionVersion) {
        this.minEncryptionVersion = minEncryptionVersion;
        return this;
    }

    public Boolean getDeletionAllowed() {
        return deletionAllowed;
    }

    public KeyConfigRequestDetail setDeletionAllowed(Boolean deletionAllowed) {
        this.deletionAllowed = deletionAllowed;
        return this;
    }

    public Boolean getExportable() {
        return exportable;
    }

    public KeyConfigRequestDetail setExportable(Boolean exportable) {
        this.exportable = exportable;
        return this;
    }

    public Boolean getAllowPlaintextBackup() {
        return allowPlaintextBackup;
    }

    public KeyConfigRequestDetail setAllowPlaintextBackup(Boolean allowPlaintextBackup) {
        this.allowPlaintextBackup = allowPlaintextBackup;
        return this;
    }
}
