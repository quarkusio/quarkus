package io.quarkus.deployment.pkg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.jboss.logging.Logger;

/**
 * JarUnsigner is used to remove the signature from a jar file.
 */
public final class JarUnsigner {

    private static final Logger log = Logger.getLogger(JarUnsigner.class);

    private JarUnsigner() {
        // utility class
    }

    /**
     * Unsigns a jar file by removing the signature entries.
     * If the JAR is not signed, it will simply copy the original JAR to the target path.
     *
     * @param jarPath the path to the jar file to unsign
     * @param targetPath the path to the target jar file
     * @throws IOException if an I/O error occurs
     */
    public static void unsignJar(Path jarPath, Path targetPath) throws IOException {
        try (JarFile in = new JarFile(jarPath.toFile(), false)) {
            Manifest manifest = in.getManifest();
            boolean signed;
            if (manifest != null) {
                Map<String, Attributes> entries = manifest.getEntries();
                signed = !entries.isEmpty();
                // Remove signature entries
                entries.clear();
            } else {
                signed = false;
                manifest = new Manifest();
            }
            if (!signed) {
                in.close();
                log.debugf("JAR %s is not signed, skipping unsigning", jarPath);
                Files.copy(jarPath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } else {
                log.debugf("JAR %s is signed, removing signature", jarPath);
                // Reusing buffer for performance reasons
                byte[] buffer = new byte[10000];
                try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(targetPath))) {
                    JarEntry manifestEntry = new JarEntry(JarFile.MANIFEST_NAME);
                    // Set manifest time to epoch to always make the same jar
                    manifestEntry.setTime(0);
                    out.putNextEntry(manifestEntry);
                    manifest.write(out);
                    out.closeEntry();
                    Enumeration<JarEntry> entries = in.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();
                        if (!entryName.equals(JarFile.MANIFEST_NAME)
                                && !entryName.equals("META-INF/INDEX.LIST")
                                && !isSignatureFile(entryName)) {
                            entry.setCompressedSize(-1);
                            out.putNextEntry(entry);
                            try (InputStream inStream = in.getInputStream(entry)) {
                                int r;
                                while ((r = inStream.read(buffer)) > 0) {
                                    out.write(buffer, 0, r);
                                }
                            } finally {
                                out.closeEntry();
                            }
                        } else {
                            log.debugf("Removed %s from %s", entryName, jarPath);
                        }
                    }
                }
            }
            // let's make sure we keep the original timestamp
            Files.setLastModifiedTime(targetPath, Files.getLastModifiedTime(jarPath));
        }
    }

    /**
     * Unsigns a jar file by removing the signature entries.
     *
     * @param jarPath the path to the jar file to unsign
     * @param targetPath the path to the target jar file
     * @param includePredicate a predicate to determine which entries to include in the target jar
     * @throws IOException if an I/O error occurs
     */
    public static void unsignJar(Path jarPath, Path targetPath, Predicate<String> includePredicate) throws IOException {
        // Reusing buffer for performance reasons
        byte[] buffer = new byte[10000];
        try (JarFile in = new JarFile(jarPath.toFile(), false)) {
            Manifest manifest = in.getManifest();
            boolean signed;
            if (manifest != null) {
                Map<String, Attributes> entries = manifest.getEntries();
                signed = !entries.isEmpty();
                // Remove signature entries
                entries.clear();
            } else {
                signed = false;
                manifest = new Manifest();
            }
            if (signed) {
                log.debugf("JAR %s is signed, removing signature", jarPath);
            }
            try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(targetPath))) {
                JarEntry manifestEntry = new JarEntry(JarFile.MANIFEST_NAME);
                // Set manifest time to epoch to always make the same jar
                manifestEntry.setTime(0);
                out.putNextEntry(manifestEntry);
                manifest.write(out);
                out.closeEntry();
                Enumeration<JarEntry> entries = in.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (includePredicate.test(entryName)
                            && !entryName.equals(JarFile.MANIFEST_NAME)
                            && !entryName.equals("META-INF/INDEX.LIST")
                            && !isSignatureFile(entryName)) {
                        entry.setCompressedSize(-1);
                        out.putNextEntry(entry);
                        try (InputStream inStream = in.getInputStream(entry)) {
                            int r;
                            while ((r = inStream.read(buffer)) > 0) {
                                out.write(buffer, 0, r);
                            }
                        } finally {
                            out.closeEntry();
                        }
                    } else {
                        log.debugf("Removed %s from %s", entryName, jarPath);
                    }
                }
            }
            // let's make sure we keep the original timestamp
            Files.setLastModifiedTime(targetPath, Files.getLastModifiedTime(jarPath));
        }
    }

    private static boolean isSignatureFile(String entry) {
        entry = entry.toUpperCase();
        if (entry.startsWith("META-INF/") && entry.indexOf('/', "META-INF/".length()) == -1) {
            return entry.endsWith(".SF")
                    || entry.endsWith(".DSA")
                    || entry.endsWith(".RSA")
                    || entry.endsWith(".EC");
        }
        return false;
    }
}
