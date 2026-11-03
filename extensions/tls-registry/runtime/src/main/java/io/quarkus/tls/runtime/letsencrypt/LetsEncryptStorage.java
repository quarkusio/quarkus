package io.quarkus.tls.runtime.letsencrypt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Stores the account key, the certificate chain and the certificate key of the Let's Encrypt client in a
 * directory. The files that hold a private key are readable by the owner only. Each file is first written to a
 * temporary file and then moved, so that a crash never leaves a partial file.
 */
final class LetsEncryptStorage {

    static final String ACCOUNT_KEY = "account-key.pem";
    static final String CERTIFICATE_CHAIN = "lets-encrypt.crt";
    static final String CERTIFICATE_KEY = "lets-encrypt.key";

    private static final Set<PosixFilePermission> OWNER_ONLY = PosixFilePermissions.fromString("rw-------");

    private final Path directory;

    /**
     * Create a storage in the given directory.
     *
     * @param directory the directory
     */
    LetsEncryptStorage(Path directory) {
        this.directory = directory;
    }

    /**
     * The stored certificate: the chain, the end-entity certificate first, and its private key.
     *
     * @param chain the certificate chain
     * @param key the private key of the end-entity certificate
     */
    record StoredCertificate(List<X509Certificate> chain, PrivateKey key) {

        X509Certificate certificate() {
            return chain.get(0);
        }
    }

    /**
     * Read the account key, or create and store a new one.
     *
     * @return the account key pair
     * @throws IOException if the file cannot be read or written
     * @throws GeneralSecurityException if the stored key is not valid
     */
    KeyPair accountKey() throws IOException, GeneralSecurityException {
        Path file = directory.resolve(ACCOUNT_KEY);
        if (Files.exists(file)) {
            return Certificates.readKeyPair(Files.readString(file));
        }
        KeyPair keyPair = Certificates.generateKeyPair();
        write(file, Certificates.keyPairPem(keyPair), true);
        return keyPair;
    }

    /**
     * Read the stored certificate.
     *
     * @return the certificate, or an empty optional if none is stored
     * @throws IOException if a file cannot be read
     * @throws GeneralSecurityException if a stored file is not valid
     */
    Optional<StoredCertificate> certificate() throws IOException, GeneralSecurityException {
        Path chain = directory.resolve(CERTIFICATE_CHAIN);
        Path key = directory.resolve(CERTIFICATE_KEY);
        if (!Files.exists(chain) || !Files.exists(key)) {
            return Optional.empty();
        }
        return Optional.of(new StoredCertificate(Certificates.readCertificates(Files.readString(chain)),
                Certificates.readPrivateKey(Files.readString(key))));
    }

    /**
     * Store a certificate. The key is written before the chain, so that a stored chain always has its key.
     *
     * @param certificate the certificate
     * @throws IOException if a file cannot be written
     * @throws GeneralSecurityException if a certificate cannot be encoded
     */
    void store(StoredCertificate certificate) throws IOException, GeneralSecurityException {
        write(directory.resolve(CERTIFICATE_KEY), Certificates.privateKeyPem(certificate.key()), true);
        write(directory.resolve(CERTIFICATE_CHAIN), Certificates.certificatesPem(certificate.chain()), false);
    }

    @Override
    public String toString() {
        return directory.toAbsolutePath().toString();
    }

    private void write(Path file, String content, boolean secret) throws IOException {
        Files.createDirectories(directory);
        boolean posix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
        Path temporary = posix && secret
                ? Files.createTempFile(directory, file.getFileName().toString(), ".tmp",
                        PosixFilePermissions.asFileAttribute(OWNER_ONLY))
                : Files.createTempFile(directory, file.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.US_ASCII);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
