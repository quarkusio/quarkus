package io.quarkus.tls.cli.letsencrypt;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.wildfly.security.x500.cert.X509CertificateChainAndSigningKey;
import org.wildfly.security.x500.cert.acme.AcmeAccount;
import org.wildfly.security.x500.cert.acme.AcmeException;

import io.smallrye.certs.CertificateUtils;
import io.vertx.core.json.JsonObject;

public class LetsEncryptHelpers {

    static System.Logger LOGGER = System.getLogger("lets-encrypt");

    public static void writePrivateKeyAndCertificateChainsAsPem(PrivateKey pk, X509Certificate[] chain, File privateKeyFile,
            File certificateChainFile) throws Exception {
        if (pk == null) {
            throw new IllegalArgumentException("The private key cannot be null");
        }
        if (chain == null || chain.length == 0) {
            throw new IllegalArgumentException("The certificate chain cannot be null or empty");
        }

        CertificateUtils.writePrivateKeyToPem(pk, null, privateKeyFile);

        if (chain.length == 1) {
            CertificateUtils.writeCertificateToPEM(chain[0], certificateChainFile);
            return;
        }

        // For some reason the method from CertificateUtils distinguishes the first certificate and the rest of the chain
        X509Certificate[] restOfTheChain = new X509Certificate[chain.length - 1];
        System.arraycopy(chain, 1, restOfTheChain, 0, chain.length - 1);
        CertificateUtils.writeCertificateToPEM(chain[0], certificateChainFile, restOfTheChain);
    }

    public static X509Certificate loadCertificateFromPEM(String pemFilePath) throws IOException, CertificateException {
        try (PemReader pemReader = new PemReader(new FileReader(pemFilePath))) {
            PemObject pemObject = pemReader.readPemObject();
            if (pemObject == null) {
                throw new IOException("Invalid PEM file: No PEM content found.");
            }
            byte[] content = pemObject.getContent();
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(content));
        }
    }

    public static String createAccount(AcmeClient acmeClient,
            String letsEncryptPath,
            boolean staging,
            String contactEmail) {
        LOGGER.log(INFO, "\uD83D\uDD35 Creating {0} Let's Encrypt account", (staging ? "staging" : "production"));

        AcmeAccount acmeAccount = AcmeAccount.builder()
                .setTermsOfServiceAgreed(true)
                .setServerUrl("https://acme-v02.api.letsencrypt.org/directory") // TODO Should this be configurable?
                .setStagingServerUrl("https://acme-staging-v02.api.letsencrypt.org/directory") // TODO Should this be configurable?
                .setContactUrls(new String[] { "mailto:" + contactEmail })
                .build();

        try {
            if (!acmeClient.createAccount(acmeAccount, staging)) {
                LOGGER.log(INFO, "\uD83D\uDD35 {0} Let's Encrypt account {1} already exists",
                        (staging ? "Staging" : "Production"),
                        contactEmail);
            } else {
                LOGGER.log(INFO, "\uD83D\uDD35 {0} Let's Encrypt account {1} has been created",
                        (staging ? "Staging" : "Production"),
                        contactEmail);
            }
        } catch (AcmeException ex) {
            LOGGER.log(ERROR, "⚠\uFE0F Failed to create Let's Encrypt account");
            throw new RuntimeException(ex);
        }

        JsonObject accountJson = convertAccountToJson(acmeAccount);
        saveAccount(letsEncryptPath, accountJson);
        return accountJson.encode();
    }

    private static JsonObject convertAccountToJson(AcmeAccount acmeAccount) {
        JsonObject json = new JsonObject();
        json.put("account-url", acmeAccount.getAccountUrl());
        json.put("contact-url", acmeAccount.getContactUrls()[0]);
        if (acmeAccount.getPrivateKey() != null) {
            json.put("private-key", new String(Base64.getEncoder().encode(acmeAccount.getPrivateKey().getEncoded()),
                    StandardCharsets.US_ASCII));
        }
        if (acmeAccount.getCertificate() != null) {
            try {
                json.put("certificate", new String(Base64.getEncoder().encode(acmeAccount.getCertificate().getEncoded()),
                        StandardCharsets.US_ASCII));
            } catch (CertificateEncodingException ex) {
                LOGGER.log(INFO, "⚠\uFE0F Failed to get encoded certificate data");
                throw new RuntimeException(ex);
            }
        }
        if (acmeAccount.getKeyAlgorithmName() != null) {
            json.put("key-algorithm", acmeAccount.getKeyAlgorithmName());
        }
        json.put("key-size", acmeAccount.getKeySize());
        return json;
    }

    private static void saveAccount(String letsEncryptPath, JsonObject accountJson) {
        LOGGER.log(DEBUG, "Saving account to {0}", letsEncryptPath);

        // If more than one account must be supported, we can save accounts to unique files in .lets-encrypt/accounts
        // and require an account alias/id during operations requiring an account
        java.nio.file.Path accountPath = Paths.get(letsEncryptPath + "/account.json");
        try {
            Files.copy(new ByteArrayInputStream(accountJson.encode().getBytes(StandardCharsets.US_ASCII)), accountPath,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Failure to save the account", ex);
        }
    }

    public static void issueCertificate(
            AcmeClient acmeClient,
            File letsEncryptPath,
            boolean staging,
            String domain,
            File certChainPemLoc,
            File privateKeyPemLoc) {
        AcmeAccount acmeAccount = getAccount(letsEncryptPath);
        X509CertificateChainAndSigningKey certChainAndPrivateKey;
        try {
            certChainAndPrivateKey = acmeClient.obtainCertificateChain(acmeAccount, staging, domain);
        } catch (AcmeException t) {
            throw new RuntimeException(t.getMessage());
        }
        LOGGER.log(INFO, "\uD83D\uDD35 Certificate and private key issued, converting them to PEM files");

        try {
            LetsEncryptHelpers.writePrivateKeyAndCertificateChainsAsPem(certChainAndPrivateKey.getSigningKey(),
                    certChainAndPrivateKey.getCertificateChain(), privateKeyPemLoc, certChainPemLoc);
        } catch (Exception ex) {
            throw new RuntimeException("Failure to copy certificate pem");
        }
    }

    private static AcmeAccount getAccount(File letsEncryptPath) {
        LOGGER.log(DEBUG, "Getting account from {0}", letsEncryptPath);

        JsonObject json = readAccountJson(letsEncryptPath);
        AcmeAccount.Builder builder = AcmeAccount.builder().setTermsOfServiceAgreed(true)
                .setServerUrl("https://acme-v02.api.letsencrypt.org/directory")
                .setStagingServerUrl("https://acme-staging-v02.api.letsencrypt.org/directory");

        String keyAlgorithm = json.getString("key-algorithm");
        builder.setKeyAlgorithmName(keyAlgorithm);
        builder.setKeySize(json.getInteger("key-size"));

        if (json.containsKey("private-key") && json.containsKey("certificate")) {
            PrivateKey privateKey = getPrivateKey(json.getString("private-key"), keyAlgorithm);
            X509Certificate certificate = getCertificate(json.getString("certificate"));

            builder.setKey(certificate, privateKey);
        }

        AcmeAccount acmeAccount = builder.build();

        acmeAccount.setContactUrls(new String[] { json.getString("contact-url") });
        acmeAccount.setAccountUrl(json.getString("account-url"));

        return acmeAccount;
    }

    private static JsonObject readAccountJson(File letsEncryptPath) {
        LOGGER.log(DEBUG, "Reading account information from {0}", letsEncryptPath);
        java.nio.file.Path accountPath = Paths.get(letsEncryptPath + "/account.json");
        try (FileInputStream fis = new FileInputStream(accountPath.toString())) {
            return new JsonObject(new String(fis.readAllBytes(), StandardCharsets.US_ASCII));
        } catch (IOException e) {
            throw new RuntimeException("Unable to read the account file, you must create account first");
        }
    }

    private static X509Certificate getCertificate(String encodedCert) {
        try {
            byte[] encodedBytes = Base64.getDecoder().decode(encodedCert);
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(encodedBytes));
        } catch (Exception ex) {
            throw new RuntimeException("Failure to create a certificate", ex);
        }
    }

    private static PrivateKey getPrivateKey(String encodedKey, String keyAlgorithm) {
        try {
            KeyFactory f = KeyFactory.getInstance((keyAlgorithm == null || "RSA".equals(keyAlgorithm) ? "RSA" : "EC"));
            byte[] encodedBytes = Base64.getDecoder().decode(encodedKey);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encodedBytes);
            return f.generatePrivate(spec);
        } catch (Exception ex) {
            throw new RuntimeException("Failure to create a private key", ex);
        }
    }

    public static void renewCertificate(AcmeClient acmeClient,
            File letsEncryptPath,
            boolean staging,
            String domain,
            File certChainPemLoc,
            File privateKeyPemLoc) {
        LOGGER.log(INFO, "\uD83D\uDD35 Renewing {0} Let's Encrypt certificate chain and private key",
                (staging ? "staging" : "production"));
        issueCertificate(acmeClient, letsEncryptPath, staging, domain, certChainPemLoc, privateKeyPemLoc);
    }

    public static void deactivateAccount(AcmeClient acmeClient, File letsEncryptPath, boolean staging) throws IOException {
        AcmeAccount acmeAccount = getAccount(letsEncryptPath);
        LOGGER.log(INFO, "Deactivating {0} Let's Encrypt account", (staging ? "staging" : "production"));
        acmeClient.deactivateAccount(acmeAccount, staging);

        LOGGER.log(INFO, "Removing account file from {0}", letsEncryptPath);

        java.nio.file.Path accountPath = Paths.get(letsEncryptPath + "/account.json");
        Files.deleteIfExists(accountPath);
    }

    public static void adjustPermissions(File certFile, File keyFile) {
        if (!certFile.setReadable(true, false)) {
            LOGGER.log(ERROR, "Failed to set certificate file readable");
        }
        if (!certFile.setWritable(true, true)) {
            LOGGER.log(ERROR, "Failed to set certificate file as not writable");
        }
        if (!keyFile.setReadable(true, false)) {
            LOGGER.log(ERROR, "Failed to set key file as readable");
        }
        if (!keyFile.setWritable(true, true)) {
            LOGGER.log(ERROR, "Failed to set key file as not writable");
        }
    }
}
