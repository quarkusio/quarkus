package io.quarkus.tls.cli.helpers;

import java.io.File;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import io.smallrye.certs.CertificateUtils;

public class LetsEncryptHelpers {

    public static void writePrivateKeyAndCertificateChainsAsPem(PrivateKey pk, X509Certificate[] chain, File privateKeyFile,
            File certificateChainFile) throws Exception {
        if (pk == null) {
            throw new IllegalArgumentException("The private key cannot be null");
        }
        if (chain == null || chain.length == 0) {
            throw new IllegalArgumentException("The certificate chain cannot be null or empty");
        }

        CertificateUtils.writePrivateKeyToPem(pk, privateKeyFile);

        if (chain.length == 1) {
            CertificateUtils.writeCertificateToPEM(chain[0], certificateChainFile);
            return;
        }

        // For some reason the method from CertificateUtils distinguishes the first certificate and the rest of the chain
        X509Certificate[] restOfTheChain = new X509Certificate[chain.length - 1];
        System.arraycopy(chain, 1, restOfTheChain, 0, chain.length - 1);
        CertificateUtils.writeCertificateToPEM(chain[0], certificateChainFile, restOfTheChain);
    }

}
