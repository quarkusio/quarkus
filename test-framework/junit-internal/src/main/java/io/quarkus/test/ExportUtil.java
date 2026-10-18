package io.quarkus.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

final class ExportUtil {

    static final String APPLICATION_PROPERTIES = "application.properties";

    private ExportUtil() {
    }

    static void exportToQuarkusDeploymentPath(JavaArchive archive) throws IOException {
        String exportPath = System.getProperty("quarkus.deploymentExportPath");
        if (exportPath == null) {
            return;
        }
        File exportDir = new File(exportPath);
        if (exportDir.exists()) {
            if (!exportDir.isDirectory()) {
                throw new IllegalStateException("Export path is not a directory: " + exportPath);
            }
            try (Stream<Path> stream = Files.walk(exportDir.toPath())) {
                stream.sorted(Comparator.reverseOrder()).map(Path::toFile)
                        .forEach(File::delete);
            }
        } else if (!exportDir.mkdirs()) {
            throw new IllegalStateException("Export path could not be created: " + exportPath);
        }
        File exportFile = new File(exportDir, archive.getName());
        archive.as(ZipExporter.class).exportTo(exportFile);
    }

    static void mergeCustomApplicationProperties(JavaArchive archive, Properties customApplicationProperties)
            throws IOException {

        if (customApplicationProperties.isEmpty()) {
            return;
        }

        Node applicationProperties = archive.get(APPLICATION_PROPERTIES);
        if (applicationProperties != null) {
            // Read all properties, including commented properties
            AllProperties originalProperties = new AllProperties();
            Asset asset = applicationProperties.getAsset();
            if (asset instanceof StringAsset strAsset) {
                originalProperties.load(new StringReader(strAsset.getSource()));
            } else {
                originalProperties.load(asset.openStream());
            }

            // A new Properties which include the commented properties. It is possible to put commented properties
            // We do it this way, since we want to use Properties#store, which writes all unicodes and escapes correctly
            Properties properties = new Properties();
            properties.putAll(originalProperties);
            properties.putAll(customApplicationProperties);

            deleteApplicationProperties(archive);
            archive.add(new PropertiesAsset(properties), APPLICATION_PROPERTIES);
        } else {
            archive.add(new PropertiesAsset(customApplicationProperties), APPLICATION_PROPERTIES);
        }
    }

    static void deleteApplicationProperties(JavaArchive archive) {
        // MemoryMapArchiveBase#addAsset(ArchivePath,Asset) does not overwrite the existing node correctly
        // https://github.com/shrinkwrap/shrinkwrap/issues/179
        archive.delete(APPLICATION_PROPERTIES);
    }

    /**
     * Mostly a copy of {@link java.util.Properties} to read the {@code application.properties} file to keeping the
     * commented keys.
     * <p>
     * When we merge {@link java.util.Properties} if the original file contains commented keys, we lose those keys,
     * but they can be used in DevMode tests by uncommenting when updating the resource.
     */
    private static class AllProperties extends Hashtable<Object, Object> {

        public synchronized void load(Reader reader) throws IOException {
            Objects.requireNonNull(reader, "reader parameter is null");
            load0(new LineReader(reader));
        }

        public synchronized void load(InputStream inStream) throws IOException {
            Objects.requireNonNull(inStream, "inStream parameter is null");
            load0(new LineReader(inStream));
        }

        private void load0(LineReader lr) throws IOException {
            StringBuilder outBuffer = new StringBuilder();
            int limit;
            int keyLen;
            int valueStart;
            boolean hasSep;
            boolean precedingBackslash;

            while ((limit = lr.readLine()) >= 0) {
                keyLen = 0;
                valueStart = limit;
                hasSep = false;

                //System.out.println("line=<" + new String(lineBuf, 0, limit) + ">");
                precedingBackslash = false;
                while (keyLen < limit) {
                    char c = lr.lineBuf[keyLen];
                    //need check if escaped.
                    if ((c == '=' || c == ':') && !precedingBackslash) {
                        valueStart = keyLen + 1;
                        hasSep = true;
                        break;
                    } else if ((c == ' ' || c == '\t' || c == '\f') && !precedingBackslash) {
                        valueStart = keyLen + 1;
                        break;
                    }
                    if (c == '\\') {
                        precedingBackslash = !precedingBackslash;
                    } else {
                        precedingBackslash = false;
                    }
                    keyLen++;
                }
                while (valueStart < limit) {
                    char c = lr.lineBuf[valueStart];
                    if (c != ' ' && c != '\t' && c != '\f') {
                        if (!hasSep && (c == '=' || c == ':')) {
                            hasSep = true;
                        } else {
                            break;
                        }
                    }
                    valueStart++;
                }
                String key = loadConvert(lr.lineBuf, 0, keyLen, outBuffer);
                if ("#".equals(key)) {
                    continue;
                }
                String value = loadConvert(lr.lineBuf, valueStart, limit - valueStart, outBuffer);
                put(key, value);
            }
        }

        private String loadConvert(char[] in, int off, int len, StringBuilder out) {
            char aChar;
            int end = off + len;
            int start = off;
            while (off < end) {
                aChar = in[off++];
                if (aChar == '\\') {
                    break;
                }
            }
            if (off == end) { // No backslash
                return new String(in, start, len);
            }

            // backslash found at off - 1, reset the shared buffer, rewind offset
            out.setLength(0);
            off--;
            out.append(in, start, off - start);

            while (off < end) {
                aChar = in[off++];
                if (aChar == '\\') {
                    // No need to bounds check since LineReader::readLine excludes
                    // unescaped \s at the end of the line
                    aChar = in[off++];
                    if (aChar == 'u') {
                        // Read the xxxx
                        if (off > end - 4)
                            throw new IllegalArgumentException(
                                    "Malformed \\uxxxx encoding.");
                        int value = 0;
                        for (int i = 0; i < 4; i++) {
                            aChar = in[off++];
                            value = switch (aChar) {
                                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> (value << 4) + aChar - '0';
                                case 'a', 'b', 'c', 'd', 'e', 'f' -> (value << 4) + 10 + aChar - 'a';
                                case 'A', 'B', 'C', 'D', 'E', 'F' -> (value << 4) + 10 + aChar - 'A';
                                default -> throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
                            };
                        }
                        out.append((char) value);
                    } else {
                        if (aChar == 't')
                            aChar = '\t';
                        else if (aChar == 'r')
                            aChar = '\r';
                        else if (aChar == 'n')
                            aChar = '\n';
                        else if (aChar == 'f')
                            aChar = '\f';
                        out.append(aChar);
                    }
                } else {
                    out.append(aChar);
                }
            }
            return out.toString();
        }

        private static class LineReader {
            LineReader(InputStream inStream) {
                this.inStream = inStream;
                inByteBuf = new byte[8192];
            }

            LineReader(Reader reader) {
                this.reader = reader;
                inCharBuf = new char[8192];
            }

            char[] lineBuf = new char[1024];
            private byte[] inByteBuf;
            private char[] inCharBuf;
            private int inLimit = 0;
            private int inOff = 0;
            private InputStream inStream;
            private Reader reader;

            int readLine() throws IOException {
                // use locals to optimize for interpreted performance
                int len = 0;
                int off = inOff;
                int limit = inLimit;

                boolean skipWhiteSpace = true;
                boolean appendedLineBegin = false;
                boolean precedingBackslash = false;
                boolean fromStream = inStream != null;
                byte[] byteBuf = inByteBuf;
                char[] charBuf = inCharBuf;
                char[] lineBuf = this.lineBuf;
                char c;

                while (true) {
                    if (off >= limit) {
                        inLimit = limit = fromStream ? inStream.read(byteBuf)
                                : reader.read(charBuf);
                        if (limit <= 0) {
                            if (len == 0) {
                                return -1;
                            }
                            return precedingBackslash ? len - 1 : len;
                        }
                        off = 0;
                    }

                    // (char)(byte & 0xFF) is equivalent to calling a ISO8859-1 decoder.
                    c = (fromStream) ? (char) (byteBuf[off++] & 0xFF) : charBuf[off++];

                    if (skipWhiteSpace) {
                        if (c == ' ' || c == '\t' || c == '\f') {
                            continue;
                        }
                        if (!appendedLineBegin && (c == '\r' || c == '\n')) {
                            continue;
                        }
                        skipWhiteSpace = false;
                        appendedLineBegin = false;

                    }
                    if (len == 0) { // Still on a new logical line
                        if (c == '!') {

                            // When checking for new line characters a range check,
                            // starting with the higher bound ('\r') means one less
                            // branch in the common case.
                            commentLoop: while (true) {
                                if (fromStream) {
                                    byte b;
                                    while (off < limit) {
                                        b = byteBuf[off++];
                                        if (b <= '\r' && (b == '\r' || b == '\n'))
                                            break commentLoop;
                                    }
                                    if (off == limit) {
                                        inLimit = limit = inStream.read(byteBuf);
                                        if (limit <= 0) { // EOF
                                            return -1;
                                        }
                                        off = 0;
                                    }
                                } else {
                                    while (off < limit) {
                                        c = charBuf[off++];
                                        if (c <= '\r' && (c == '\r' || c == '\n'))
                                            break commentLoop;
                                    }
                                    if (off == limit) {
                                        inLimit = limit = reader.read(charBuf);
                                        if (limit <= 0) { // EOF
                                            return -1;
                                        }
                                        off = 0;
                                    }
                                }
                            }
                            skipWhiteSpace = true;
                            continue;
                        }
                    }

                    if (c != '\n' && c != '\r') {
                        lineBuf[len++] = c;
                        if (len == lineBuf.length) {
                            int newLength = lineBuf.length * 2;
                            if (newLength < 0) {
                                newLength = Integer.MAX_VALUE;
                            }
                            char[] buf = new char[newLength];
                            System.arraycopy(lineBuf, 0, buf, 0, lineBuf.length);
                            lineBuf = buf;
                        }
                        // flip the preceding backslash flag
                        precedingBackslash = (c == '\\') ? !precedingBackslash : false;
                    } else {
                        // reached EOL
                        if (len == 0) {
                            skipWhiteSpace = true;
                            continue;
                        }
                        if (off >= limit) {
                            inLimit = limit = fromStream ? inStream.read(byteBuf)
                                    : reader.read(charBuf);
                            off = 0;
                            if (limit <= 0) { // EOF
                                return precedingBackslash ? len - 1 : len;
                            }
                        }
                        if (precedingBackslash) {
                            // backslash at EOL is not part of the line
                            len -= 1;
                            // skip leading whitespace characters in the following line
                            skipWhiteSpace = true;
                            appendedLineBegin = true;
                            precedingBackslash = false;
                            // take care not to include any subsequent \n
                            if (c == '\r') {
                                if (fromStream) {
                                    if (byteBuf[off] == '\n') {
                                        off++;
                                    }
                                } else {
                                    if (charBuf[off] == '\n') {
                                        off++;
                                    }
                                }
                            }
                        } else {
                            inOff = off;
                            return len;
                        }
                    }
                }
            }
        }
    }
}
