package io.quarkus.vault.pki;

import java.io.ByteArrayInputStream;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class X509Parsing {

    private static final CertificateFactory certificateFactory;
    static {
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException x) {
            throw new RuntimeException(x);
        }
    }

    private static final Pattern PEM_CERT_REGEX = Pattern
            .compile("-+\\s*BEGIN\\s*CERTIFICATE\\s*-+\\s+([A-Za-z0-9+/=\\n\\s]+)-+\\s*END\\s*CERTIFICATE\\s*-+\\s*");
    private static final Pattern PEM_CRL_REGEX = Pattern
            .compile("-+\\s*BEGIN\\s+X509\\s+CRL\\s*-+\\s+([A-Za-z0-9+/=\\n\\s]+)-+\\s*END\\s+X509\\s+CRL\\s*-+\\s*");
    private static final int PEM_REGEX_CONTENT_GROUP = 1;

    private static final Base64.Decoder BASE64_DECODER = Base64.getMimeDecoder();

    static X509Certificate parsePEMCertificate(String pem) throws CertificateException {
        Matcher pemMatcher = PEM_CERT_REGEX.matcher(pem);
        if (!pemMatcher.matches()) {
            throw new CertificateException("Invalid PEM Certificate");
        }
        byte[] certificateData = BASE64_DECODER.decode(pemMatcher.group(PEM_REGEX_CONTENT_GROUP));
        return parseDERCertificate(certificateData);
    }

    static X509Certificate parseDERCertificate(byte[] certificateData) throws CertificateException {
        return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certificateData));
    }

    static List<X509Certificate> parsePEMCertificates(String pem) throws CertificateException {
        Matcher pemMatcher = PEM_CERT_REGEX.matcher(pem);
        List<X509Certificate> certificates = new ArrayList<>();
        while (pemMatcher.find()) {
            String base64CertificateData = pemMatcher.group(PEM_REGEX_CONTENT_GROUP);
            byte[] certificateData = BASE64_DECODER.decode(base64CertificateData);
            certificates.add(parseDERCertificate(certificateData));
        }
        return certificates;
    }

    static List<X509Certificate> parseDERCertificates(byte[] certificatesData) throws CertificateException {
        List<X509Certificate> certificates = new ArrayList<>();
        for (Certificate certificate : certificateFactory.generateCertificates(new ByteArrayInputStream(certificatesData))) {
            certificates.add((X509Certificate) certificate);
        }
        return certificates;
    }

    static X509CRL parsePEMCRL(String pem) throws CRLException {
        Matcher pemMatcher = PEM_CRL_REGEX.matcher(pem);
        if (!pemMatcher.matches()) {
            throw new CRLException("Invalid PEM CRL");
        }
        byte[] crlData = BASE64_DECODER.decode(pemMatcher.group(PEM_REGEX_CONTENT_GROUP));
        return parseDERCRL(crlData);
    }

    static X509CRL parseDERCRL(byte[] crlData) throws CRLException {
        return (X509CRL) certificateFactory.generateCRL(new ByteArrayInputStream(crlData));
    }

}
