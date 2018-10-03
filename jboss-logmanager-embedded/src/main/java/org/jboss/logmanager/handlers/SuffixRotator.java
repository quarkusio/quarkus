/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.logmanager.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.ErrorManager;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A utility for rotating files based on a files suffix.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class SuffixRotator {

    /**
     * The compression type for the rotation
     */
    public enum CompressionType {
        NONE,
        GZIP,
        ZIP
    }

    /**
     * An empty rotation suffix.
     */
    static final SuffixRotator EMPTY = new SuffixRotator("", "", "", CompressionType.NONE);

    private final String originalSuffix;
    private final String datePattern;
    private final SimpleDateFormat formatter;
    private final String compressionSuffix;
    private final CompressionType compressionType;

    private SuffixRotator(final String originalSuffix, final String datePattern, final String compressionSuffix, final CompressionType compressionType) {
        this.originalSuffix = originalSuffix;
        this.datePattern = datePattern;
        this.compressionSuffix = compressionSuffix;
        this.compressionType = compressionType;
        if (datePattern.isEmpty()) {
            formatter = null;
        } else {
            formatter = new SimpleDateFormat(datePattern);
        }
    }

    /**
     * Parses a suffix into possible parts for rotation.
     *
     * @param suffix the suffix to parse
     *
     * @return the file rotator used to determine the suffix parts and rotate the file.
     */
    static SuffixRotator parse(final String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return EMPTY;
        }
        // Check the if the suffix contains a compression suffix
        String compressionSuffix = "";
        String datePattern = "";
        CompressionType compressionType = CompressionType.NONE;
        final String lSuffix = suffix.toLowerCase(Locale.ROOT);
        int compressionIndex = lSuffix.indexOf(".gz");
        if (compressionIndex != -1) {
            compressionSuffix = suffix.substring(compressionIndex);
            datePattern = suffix.substring(0, compressionIndex);
            compressionType = SuffixRotator.CompressionType.GZIP;
        } else {
            compressionIndex = lSuffix.indexOf(".zip");
            if (compressionIndex != -1) {
                compressionSuffix = suffix.substring(compressionIndex);
                datePattern = suffix.substring(0, compressionIndex);
                compressionType = SuffixRotator.CompressionType.ZIP;
            }
        }
        if (compressionSuffix.isEmpty() && datePattern.isEmpty()) {
            return new SuffixRotator(suffix, suffix, "", CompressionType.NONE);
        }
        return new SuffixRotator(suffix, datePattern, compressionSuffix, compressionType);
    }

    /**
     * The {@linkplain java.text.SimpleDateFormat date format pattern} for the suffix or an empty
     * {@linkplain String string}.
     *
     * @return the date pattern or an empty string
     */
    String getDatePattern() {
        return datePattern;
    }

    /**
     * The compression suffix or an empty {@linkplain String string}
     *
     * @return the compression suffix or an empty string
     */
    @SuppressWarnings("unused")
    String getCompressionSuffix() {
        return compressionSuffix;
    }

    /**
     * The compression type.
     *
     * @return the compression type
     */
    @SuppressWarnings("unused")
    CompressionType getCompressionType() {
        return compressionType;
    }

    /**
     * Rotates the file to a new file appending the suffix to the target.
     * <p>
     * The compression suffix will automatically be appended to target file if compression is being used. If compression
     * is not being used the file is just moved replacing the target file if it already exists.
     * </p>
     *
     * @param errorManager the error manager used to report errors to, if {@code null} an {@link IOException} will
     *                     be thrown
     * @param source       the file to be rotated
     * @param suffix       the suffix to append to the rotated file.
     */
    void rotate(final ErrorManager errorManager, final Path source, final String suffix) {
        final Path target = Paths.get(source + suffix + compressionSuffix);
        if (compressionType == CompressionType.GZIP) {
            try {
                archiveGzip(source, target);
            } catch (Exception e) {
                errorManager.error(String.format("Failed to compress %s to %s. Compressed file may be left on the " +
                        "filesystem corrupted.", source, target), e, ErrorManager.WRITE_FAILURE);
            }
        } else if (compressionType == CompressionType.ZIP) {
            try {
                archiveZip(source, target);
            } catch (Exception e) {
                errorManager.error(String.format("Failed to compress %s to %s. Compressed file may be left on the " +
                        "filesystem corrupted.", source, target), e, ErrorManager.WRITE_FAILURE);
            }
        } else {
            move(errorManager, source, target);
        }
    }

    /**
     * Rotates the file to a new file appending the suffix to the target. If a date suffix was specified the suffix
     * will be added before the index or compression suffix. The current date will be used for the suffix.
     * <p>
     * If the {@code maxBackupIndex} is greater than 0 previously rotated files will be moved to an numerically
     * incremented target. The compression suffix, if required, will be appended to this indexed file name.
     * </p>
     *
     * @param errorManager   the error manager used to report errors to, if {@code null} an {@link IOException} will
     *                       be thrown
     * @param source         the file to be rotated
     * @param maxBackupIndex the number of backups to keep
     */
    void rotate(final ErrorManager errorManager, final Path source, final int maxBackupIndex) {
        if (formatter == null) {
            rotate(errorManager, source, "", maxBackupIndex);
        } else {
            final String suffix;
            synchronized (formatter) {
                suffix = formatter.format(new Date());
            }
            rotate(errorManager, source, suffix, maxBackupIndex);
        }
    }

    /**
     * Rotates the file to a new file appending the suffix to the target.
     * <p>
     * If the {@code maxBackupIndex} is greater than 0 previously rotated files will be moved to an numerically
     * incremented target. The compression suffix, if required, will be appended to this indexed file name.
     * </p>
     *
     * @param errorManager   the error manager used to report errors to, if {@code null} an {@link IOException} will
     *                       be thrown
     * @param source         the file to be rotated
     * @param suffix         the optional suffix to append to the file before the index and optional compression suffix
     * @param maxBackupIndex the number of backups to keep
     */
    void rotate(final ErrorManager errorManager, final Path source, final String suffix, final int maxBackupIndex) {
        if (maxBackupIndex > 0) {
            final String rotationSuffix = (suffix == null ? "" : suffix);
            final String fileWithSuffix = source.toAbsolutePath() + rotationSuffix;
            final Path lastFile = Paths.get(fileWithSuffix + "." + maxBackupIndex + compressionSuffix);
            try {
                Files.deleteIfExists(lastFile);
            } catch (Exception e) {
                errorManager.error(String.format("Failed to delete file %s", lastFile), e, ErrorManager.GENERIC_FAILURE);
            }
            for (int i = maxBackupIndex - 1; i >= 1; i--) {
                final Path src = Paths.get(fileWithSuffix + "." + i + compressionSuffix);
                if (Files.exists(src)) {
                    final Path target = Paths.get(fileWithSuffix + "." + (i + 1) + compressionSuffix);
                    move(errorManager, src, target);
                }
            }
            rotate(errorManager, source, rotationSuffix + ".1");
        } else if (suffix != null && !suffix.isEmpty()) {
            rotate(errorManager, source, suffix);
        }
    }

    @Override
    public String toString() {
        return originalSuffix;
    }

    private void move(final ErrorManager errorManager, final Path src, final Path target) {
        try {
            Files.move(src, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            // Report the error, but allow the rotation to continue
            errorManager.error(String.format("Failed to move file %s to %s.", src, target), e, ErrorManager.GENERIC_FAILURE);
        }
    }


    private static void archiveGzip(final Path source, final Path target) throws IOException {
        final byte[] buff = new byte[512];
        try (final GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(target), true)) {
            try (final InputStream in = Files.newInputStream(source)) {
                int len;
                while ((len = in.read(buff)) != -1) {
                    out.write(buff, 0, len);
                }
            }
            out.finish();
        }
    }

    private static void archiveZip(final Path source, final Path target) throws IOException {
        final byte[] buff = new byte[512];
        try (final ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target), StandardCharsets.UTF_8)) {
            final ZipEntry entry = new ZipEntry(source.getFileName().toString());
            out.putNextEntry(entry);
            try (final InputStream in = Files.newInputStream(source)) {
                int len;
                while ((len = in.read(buff)) != -1) {
                    out.write(buff, 0, len);
                }
            }
            out.closeEntry();
        }
    }
}
