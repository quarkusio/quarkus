package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jdk.security.jarsigner.JarSigner;

/**
 * Test for {@link JarResultBuildStep}
 */
class JarResultBuildStepTest {

    @Test
    void should_unsign_jar_when_filtered(@TempDir Path tempDir) throws Exception {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "myarchive.jar")
                .addClasses(Integer.class);
        Path unsignedJarPath = tempDir.resolve("unsigned-jar.jar");
        Path signedJarPath = tempDir.resolve("signed-jar.jar");
        Path unsignedJarToTestPath = tempDir.resolve("unsigned.jar");
        archive.as(ZipExporter.class).exportTo(new File(unsignedJarPath.toUri()), true);
        JarSigner signer = new JarSigner.Builder(createPrivateKeyEntry()).build();
        try (ZipFile in = new ZipFile(unsignedJarPath.toFile());
                FileOutputStream out = new FileOutputStream(signedJarPath.toFile())) {
            signer.sign(in, out);
        }
        JarResultBuildStep.filterJarFile(signedJarPath, unsignedJarToTestPath, Set.of("java/lang/Integer.class"));
        try (JarFile jarFile = new JarFile(unsignedJarToTestPath.toFile())) {
            assertThat(jarFile.stream().map(JarEntry::getName)).doesNotContain("META-INF/ECLIPSE_.RSA", "META-INF/ECLIPSE_.SF");
            // Check that the manifest is still present
            Manifest manifest = jarFile.getManifest();
            assertThat(manifest.getMainAttributes()).isNotEmpty();
            assertThat(manifest.getEntries()).isEmpty();
        }
    }

    @Test
    void manifestTimeShouldAlwaysBeSetToEpoch(@TempDir Path tempDir) throws Exception {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "myarchive.jar")
                .addClasses(Integer.class)
                .addManifest();
        Path initialJar = tempDir.resolve("initial.jar");
        Path filteredJar = tempDir.resolve("filtered.jar");
        archive.as(ZipExporter.class).exportTo(new File(initialJar.toUri()), true);
        JarResultBuildStep.filterJarFile(initialJar, filteredJar, Set.of("java/lang/Integer.class"));
        try (JarFile jarFile = new JarFile(filteredJar.toFile())) {
            assertThat(jarFile.stream())
                    .filteredOn(jarEntry -> jarEntry.getName().equals(JarFile.MANIFEST_NAME))
                    .isNotEmpty()
                    .allMatch(jarEntry -> jarEntry.getTime() == 0);
            // Check that the manifest is still has attributes
            Manifest manifest = jarFile.getManifest();
            assertThat(manifest.getMainAttributes()).isNotEmpty();
        }
    }

    private static KeyStore.PrivateKeyEntry createPrivateKeyEntry()
            throws NoSuchAlgorithmException, CertificateException, OperatorCreationException, CertIOException {
        KeyPairGenerator ky = KeyPairGenerator.getInstance("RSA");
        ky.initialize(2048);
        KeyPair keyPair = ky.generateKeyPair();
        Certificate[] chain = { createCertificate(keyPair, "cn=Unknown") };
        KeyStore.PrivateKeyEntry keyEntry = new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), chain);
        return keyEntry;
    }

    private static Certificate createCertificate(KeyPair keyPair, String subjectDN)
            throws OperatorCreationException, CertificateException, CertIOException {
        Provider bcProvider = new BouncyCastleProvider();
        Security.addProvider(bcProvider);
        long now = System.currentTimeMillis();
        X500Name dnName = new X500Name(subjectDN);
        BigInteger certSerialNumber = new BigInteger(Long.toString(now));
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        Date endDate = calendar.getTime();
        String signatureAlgorithm = "SHA256WithRSA";

        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.getPrivate());

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(dnName, certSerialNumber, new Date(now),
                endDate,
                dnName, keyPair.getPublic());

        BasicConstraints basicConstraints = new BasicConstraints(true);

        certBuilder.addExtension(new ASN1ObjectIdentifier("2.5.29.19"), true, basicConstraints);
        return new JcaX509CertificateConverter().setProvider(bcProvider).getCertificate(certBuilder.build(contentSigner));

    }

}
