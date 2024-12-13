package io.quarkus.it.keycloak;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Map;

import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.smallrye.certs.chain.CertificateChainGenerator;
import io.smallrye.jwt.util.KeyUtils;

public class CustomOidcWiremockTestResource extends OidcWiremockTestResource {
    @Override
    public Map<String, String> start() {
        try {
            generateCertificates();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return super.start();
    }

    private void generateCertificates() throws Exception {
        File chainDir = new File("target/chain");
        CertificateChainGenerator chainGenerator = new CertificateChainGenerator(chainDir)
                .withCN("www.quarkustest.com");
        chainGenerator.generate();

        Path rootCertPath = Paths.get("target/chain/root.crt");
        X509Certificate rootCert = KeyUtils.getCertificate(Files.readString(rootCertPath));

        Path leafCertPath = Paths.get("target/chain/www.quarkustest.com.crt");
        X509Certificate leafCert = KeyUtils.getCertificate(Files.readString(leafCertPath));

        File trustStore = new File(chainDir, "truststore.p12");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setCertificateEntry("root", rootCert);
        keyStore.setCertificateEntry("leaf", leafCert);
        var fos = new FileOutputStream(trustStore);
        keyStore.store(fos, "storepassword".toCharArray());
        fos.close();

        File trustStoreRoot = new File(chainDir, "truststore-rootcert.p12");
        KeyStore keyStoreRootCert = KeyStore.getInstance("PKCS12");
        keyStoreRootCert.load(null, null);
        keyStoreRootCert.setCertificateEntry("root", rootCert);
        var fosRootCert = new FileOutputStream(trustStoreRoot);
        keyStoreRootCert.store(fosRootCert, "storepassword".toCharArray());
        fosRootCert.close();

    }

}
