package io.quarkus.tls.cli;

import static io.quarkus.tls.cli.Constants.CA_FILE;
import static io.quarkus.tls.cli.Constants.PK_FILE;
import static io.quarkus.tls.cli.DotEnvHelper.addOrReplaceProperty;
import static io.quarkus.tls.cli.DotEnvHelper.readDotEnvFile;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.DOT_ENV_FILE;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;
import io.smallrye.common.os.OS;
import picocli.CommandLine;

@CommandLine.Command(name = "generate-certificate", mixinStandardHelpOptions = true, description = "Generate a TLS certificate with the Quarkus Dev CA if available.")
public class GenerateCertificateCommand implements Callable<Integer> {

    @CommandLine.Option(names = { "-n",
            "--name" }, description = "Name of the certificate. It will be used as file name and alias in the keystore", required = true)
    String name;

    @CommandLine.Option(names = { "-p",
            "--password" }, description = "The password of the keystore. Default is 'password'", defaultValue = "password", required = false)
    String password;

    @CommandLine.Option(names = { "-c",
            "--cn" }, description = "The common name of the certificate. Default is 'localhost'", defaultValue = "localhost", required = false)
    String cn;

    @CommandLine.Option(names = { "-d",
            "--directory" }, description = "The directory in which the certificates will be created. Default is `.certs`", defaultValue = ".certs")
    Path directory;

    @CommandLine.Option(names = { "-r",
            "--renew" }, description = "Whether existing certificates will need to be replaced", defaultValue = "false")
    boolean renew;

    @CommandLine.Option(names = {
            "--self-signed" }, description = "Generate a self-signed certificate", defaultValue = "false", hidden = true)
    boolean selfSigned;

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    static System.Logger LOGGER = System.getLogger("generate-certificate");

    @Override
    public Integer call() throws Exception {
        LOGGER.log(INFO, "\uD83D\uDD0E Looking for the Quarkus Dev CA certificate...");

        if (!CA_FILE.exists() || !PK_FILE.exists() || selfSigned) {
            LOGGER.log(INFO, "\uD83C\uDFB2 Quarkus Dev CA certificate not found. Generating a self-signed certificate...");
            generateSelfSignedCertificate();
            return 0;
        }

        LOGGER.log(INFO, "\uD83D\uDCDC Quarkus Dev CA certificate found at {0}", CA_FILE.getAbsolutePath());
        X509Certificate caCert = loadRootCertificate(CA_FILE);
        PrivateKey caPrivateKey = loadPrivateKey();

        createSignedCertificate(caCert, caPrivateKey);

        LOGGER.log(INFO, "✅ Signed Certificate generated successfully and exported into `{0}-keystore.p12`", name);
        printConfig(directory.resolve(name + "-keystore.p12"), password);

        return 0;
    }

    private void generateSelfSignedCertificate() throws Exception {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        new CertificateGenerator(directory, renew).generate(new CertificateRequest()
                .withName(name)
                .withCN(cn)
                .withPassword(password)
                .withDuration(Duration.ofDays(365))
                .withFormat(Format.PKCS12));
        LOGGER.log(INFO, "✅ Self-signed certificate generated successfully and exported into `{0}-keystore.p12`", name);
        printConfig(directory.resolve(name + "-keystore.p12"), password);

    }

    private void printConfig(Path certificatePath, String password) {
        String certificatePathProperty = certificatePath.toString();
        if (OS.WINDOWS.isCurrent()) {
            certificatePathProperty = certificatePathProperty.replace("\\", "\\\\");
        }

        try {
            List<String> dotEnvContent = readDotEnvFile();
            addOrReplaceProperty(dotEnvContent, "%dev.quarkus.tls.key-store.p12.path", certificatePathProperty);
            addOrReplaceProperty(dotEnvContent, "%dev.quarkus.tls.key-store.p12.password", password);
            Files.write(DOT_ENV_FILE.toPath(), dotEnvContent);
        } catch (IOException e) {
            LOGGER.log(ERROR, "Failed to read .env file", e);
        }

        LOGGER.log(INFO, """
                ✅ Required configuration added to the `.env` file:
                %dev.quarkus.tls.key-store.p12.path={0}
                %dev.quarkus.tls.key-store.p12.password={1}
                """, certificatePathProperty, password);
    }

    private X509Certificate loadRootCertificate(File ca) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (FileInputStream fis = new FileInputStream(ca)) {
            return (X509Certificate) cf.generateCertificate(fis);
        }
    }

    private PrivateKey loadPrivateKey() throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(Constants.PK_FILE));
                PEMParser pemParser = new PEMParser(reader)) {
            Object obj = pemParser.readObject();
            if (obj instanceof KeyPair) {
                return ((KeyPair) obj).getPrivate();
            } else if (obj instanceof PrivateKeyInfo) {
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                return converter.getPrivateKey(((PrivateKeyInfo) obj));
            } else {
                throw new IllegalStateException(
                        "The file " + Constants.PK_FILE.getAbsolutePath() + " does not contain a private key "
                                + obj.getClass().getName());
            }
        }
    }

    private void createSignedCertificate(X509Certificate issuerCert,
            PrivateKey issuerPrivateKey) throws Exception {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        new CertificateGenerator(directory, renew).generate(new CertificateRequest()
                .withName(name)
                .withCN(cn)
                .withPassword(password)
                .withDuration(Duration.ofDays(365))
                .withFormat(Format.PKCS12)
                .signedWith(issuerCert, issuerPrivateKey));

    }
}
