package io.quarkus.vertx.http.proxy;

import static io.quarkus.vertx.http.proxy.AbstractTrustedProxyDnTest.PASSWORD;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import io.smallrye.certs.chain.CertificateChainGenerator;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.PfxOptions;

final class TrustedProxyCertChainHelper {

    private TrustedProxyCertChainHelper() {
    }

    static void generateTwoChains(File baseDir, TwoChainConsumer consumer) {
        try {
            deleteDir(baseDir);
            File chain1Dir = new File(baseDir, "chain1");
            File chain2Dir = new File(baseDir, "chain2");
            chain1Dir.mkdirs();
            chain2Dir.mkdirs();

            new CertificateChainGenerator(chain1Dir).withCN("proxy").withSAN(List.of("DNS:localhost")).generate();
            new CertificateChainGenerator(chain2Dir).withCN("proxy").withSAN(List.of("DNS:localhost")).generate();

            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            X509Certificate root1 = loadCert(cf, new File(chain1Dir, "root.crt"));
            X509Certificate inter1 = loadCert(cf, new File(chain1Dir, "intermediate.crt"));
            X509Certificate leaf1 = loadCert(cf, new File(chain1Dir, "proxy.crt"));
            PrivateKey leafKey1 = loadPkcs8Key(new File(chain1Dir, "proxy.key"));

            X509Certificate root2 = loadCert(cf, new File(chain2Dir, "root.crt"));
            X509Certificate inter2 = loadCert(cf, new File(chain2Dir, "intermediate.crt"));
            X509Certificate leaf2 = loadCert(cf, new File(chain2Dir, "proxy.crt"));
            PrivateKey leafKey2 = loadPkcs8Key(new File(chain2Dir, "proxy.key"));

            java.security.cert.Certificate[] fullChain1 = { leaf1, inter1, root1 };
            java.security.cert.Certificate[] fullChain2 = { leaf2, inter2, root2 };

            consumer.accept(root1, fullChain1, leafKey1, root2, fullChain2, leafKey2);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void buildPkcs12(File file, KeyStorePopulator populator) throws Exception {
        char[] pass = PASSWORD.toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        populator.populate(ks, pass);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            ks.store(fos, pass);
        }
    }

    static void writePem(File file, X509Certificate cert) throws Exception {
        writePem(file, new X509Certificate[] { cert });
    }

    static void writePem(File file, X509Certificate[] certs) throws Exception {
        try (FileWriter fw = new FileWriter(file)) {
            for (X509Certificate cert : certs) {
                fw.write("-----BEGIN CERTIFICATE-----\n");
                fw.write(Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(cert.getEncoded()));
                fw.write("\n-----END CERTIFICATE-----\n");
            }
        }
    }

    static void writeKeyPem(File file, PrivateKey key) throws Exception {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("-----BEGIN PRIVATE KEY-----\n");
            fw.write(Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(key.getEncoded()));
            fw.write("\n-----END PRIVATE KEY-----\n");
        }
    }

    static void deleteDir(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDir(f);
                    } else {
                        f.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    static String requestWithClientKeystore(Vertx vertx, URL tlsUrl, File keystoreFile, File clientTruststore) {
        var options = new HttpClientOptions()
                .setSsl(true)
                .setDefaultPort(tlsUrl.getPort())
                .setDefaultHost(tlsUrl.getHost())
                .setKeyCertOptions(
                        new PfxOptions()
                                .setPath(keystoreFile.getPath())
                                .setPassword(PASSWORD))
                .setTrustOptions(
                        new PfxOptions()
                                .setPath(clientTruststore.getPath())
                                .setPassword(PASSWORD));

        var client = vertx.createHttpClient(options);
        try {
            return client
                    .request(HttpMethod.GET, "/trusted-proxy")
                    .map(req -> req.putHeader("Forwarded", "proto=https;for=backend:4444;host=somehost"))
                    .flatMap(HttpClientRequest::send)
                    .flatMap(HttpClientResponse::body)
                    .map(Buffer::toString)
                    .toCompletionStage().toCompletableFuture().join();
        } finally {
            client.close().toCompletionStage().toCompletableFuture().join();
        }
    }

    private static X509Certificate loadCert(CertificateFactory cf, File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            return (X509Certificate) cf.generateCertificate(fis);
        }
    }

    private static PrivateKey loadPkcs8Key(File file) throws Exception {
        String pem = Files.readString(file.toPath());
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    @FunctionalInterface
    interface KeyStorePopulator {
        void populate(KeyStore ks, char[] password) throws Exception;
    }

    @FunctionalInterface
    interface TwoChainConsumer {
        void accept(X509Certificate root1, java.security.cert.Certificate[] fullChain1, PrivateKey leafKey1,
                X509Certificate root2, java.security.cert.Certificate[] fullChain2, PrivateKey leafKey2) throws Exception;
    }
}
