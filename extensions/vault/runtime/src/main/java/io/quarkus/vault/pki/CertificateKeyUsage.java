package io.quarkus.vault.pki;

public enum CertificateKeyUsage {
    DigitalSignature,
    ContentCommitment,
    KeyEncipherment,
    DataEncipherment,
    KeyAgreement,
    CertSign,
    CRLSign,
    EncipherOnly,
    DecipherOnly
}
