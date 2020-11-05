package io.quarkus.vault.transit;

public class KeyCreationRequestDetail {

    public String convergentEncryption;
    public Boolean derived;
    public Boolean exportable;
    public Boolean allowPlaintextBackup;
    public String type;

    public String getConvergentEncryption() {
        return convergentEncryption;
    }

    public KeyCreationRequestDetail setConvergentEncryption(String convergentEncryption) {
        this.convergentEncryption = convergentEncryption;
        return this;
    }

    public Boolean getDerived() {
        return derived;
    }

    public KeyCreationRequestDetail setDerived(Boolean derived) {
        this.derived = derived;
        return this;
    }

    public Boolean getExportable() {
        return exportable;
    }

    public KeyCreationRequestDetail setExportable(Boolean exportable) {
        this.exportable = exportable;
        return this;
    }

    public Boolean getAllowPlaintextBackup() {
        return allowPlaintextBackup;
    }

    public KeyCreationRequestDetail setAllowPlaintextBackup(Boolean allowPlaintextBackup) {
        this.allowPlaintextBackup = allowPlaintextBackup;
        return this;
    }

    public String getType() {
        return type;
    }

    public KeyCreationRequestDetail setType(String type) {
        this.type = type;
        return this;
    }
}
