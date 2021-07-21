package io.quarkus.vault.pki;

public enum CertificateExtendedKeyUsage {
    ServerAuth,
    ClientAuth,
    CodeSigning,
    EmailProtection,
    IPSECEndSystem,
    IPSECTunnel,
    IPSECUser,
    TimeStamping,
    OCSPSigning,
    MicrosoftServerGatedCrypto,
    NetscapeServerGatedCrypto,
    MicrosoftCommercialCodeSigning,
    MicrosoftKernelCodeSigning
}
