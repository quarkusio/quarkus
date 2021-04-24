package io.quarkus.jbang;

import static io.quarkus.bootstrap.util.PropertyUtils.isWindows;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.Assertions;

import io.quarkus.deployment.util.IoUtil;

public class TestHelper {

    static public int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    // Taken from jbang - generic method that handles unzipping a jar, strip root folder and deals with
    // setting proper permissions on the resulting folders.
    public static void unzip(Path zip, Path outputDir, boolean stripRootFolder, Path selectFolder) throws IOException {
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry zipEntry = entries.nextElement();
                Path entry = Paths.get(zipEntry.getName());
                if (stripRootFolder) {
                    if (entry.getNameCount() == 1) {
                        continue;
                    }
                    entry = entry.subpath(1, entry.getNameCount());
                }
                if (selectFolder != null) {
                    if (!entry.startsWith(selectFolder) || entry.equals(selectFolder)) {
                        continue;
                    }
                    entry = entry.subpath(selectFolder.getNameCount(), entry.getNameCount());
                }
                entry = outputDir.resolve(entry).normalize();
                if (!entry.startsWith(outputDir)) {
                    throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
                }
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(entry);
                } else if (zipEntry.isUnixSymlink()) {
                    Scanner s = new Scanner(zipFile.getInputStream(zipEntry)).useDelimiter("\\A");
                    String result = s.hasNext() ? s.next() : "";
                    Files.createSymbolicLink(entry, Paths.get(result));
                } else {
                    if (!Files.isDirectory(entry.getParent())) {
                        Files.createDirectories(entry.getParent());
                    }
                    try (InputStream zis = zipFile.getInputStream(zipEntry)) {
                        Files.copy(zis, entry);
                    }
                    int mode = zipEntry.getUnixMode();
                    if (mode != 0 && !isWindows()) {
                        Set<PosixFilePermission> permissions = PosixFilePermissionSupport.toPosixFilePermissions(mode);
                        Files.setPosixFilePermissions(entry, permissions);
                    }
                }
            }
        }
    }

    static String performRequest(String path, int expectedCode) {
        try {
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // the default Accept header used by HttpURLConnection is not compatible with RESTEasy negotiation as it uses q=.2
            connection.setRequestProperty("Accept", "text/html, *; q=0.2, */*; q=0.2");
            if (connection.getResponseCode() != expectedCode) {
                Assertions.fail("Invalid response code " + connection.getResponseCode());
            }
            return new String(IoUtil.readBytes(connection.getInputStream()), StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            if (expectedCode == 404) {
                return "";
            }
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class PosixFilePermissionSupport {

        private static final int OWNER_READ_FILEMODE = 0b100_000_000;
        private static final int OWNER_WRITE_FILEMODE = 0b010_000_000;
        private static final int OWNER_EXEC_FILEMODE = 0b001_000_000;

        private static final int GROUP_READ_FILEMODE = 0b000_100_000;
        private static final int GROUP_WRITE_FILEMODE = 0b000_010_000;
        private static final int GROUP_EXEC_FILEMODE = 0b000_001_000;

        private static final int OTHERS_READ_FILEMODE = 0b000_000_100;
        private static final int OTHERS_WRITE_FILEMODE = 0b000_000_010;
        private static final int OTHERS_EXEC_FILEMODE = 0b000_000_001;

        private PosixFilePermissionSupport() {
        }

        static Set<PosixFilePermission> toPosixFilePermissions(int octalFileMode) {
            Set<PosixFilePermission> permissions = new LinkedHashSet<>();
            // Owner
            if ((octalFileMode & OWNER_READ_FILEMODE) == OWNER_READ_FILEMODE) {
                permissions.add(PosixFilePermission.OWNER_READ);
            }
            if ((octalFileMode & OWNER_WRITE_FILEMODE) == OWNER_WRITE_FILEMODE) {
                permissions.add(PosixFilePermission.OWNER_WRITE);
            }
            if ((octalFileMode & OWNER_EXEC_FILEMODE) == OWNER_EXEC_FILEMODE) {
                permissions.add(PosixFilePermission.OWNER_EXECUTE);
            }
            // Group
            if ((octalFileMode & GROUP_READ_FILEMODE) == GROUP_READ_FILEMODE) {
                permissions.add(PosixFilePermission.GROUP_READ);
            }
            if ((octalFileMode & GROUP_WRITE_FILEMODE) == GROUP_WRITE_FILEMODE) {
                permissions.add(PosixFilePermission.GROUP_WRITE);
            }
            if ((octalFileMode & GROUP_EXEC_FILEMODE) == GROUP_EXEC_FILEMODE) {
                permissions.add(PosixFilePermission.GROUP_EXECUTE);
            }
            // Others
            if ((octalFileMode & OTHERS_READ_FILEMODE) == OTHERS_READ_FILEMODE) {
                permissions.add(PosixFilePermission.OTHERS_READ);
            }
            if ((octalFileMode & OTHERS_WRITE_FILEMODE) == OTHERS_WRITE_FILEMODE) {
                permissions.add(PosixFilePermission.OTHERS_WRITE);
            }
            if ((octalFileMode & OTHERS_EXEC_FILEMODE) == OTHERS_EXEC_FILEMODE) {
                permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            }
            return permissions;
        }
    }
}
