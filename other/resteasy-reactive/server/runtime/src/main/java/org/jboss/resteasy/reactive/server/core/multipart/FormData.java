package org.jboss.resteasy.reactive.server.core.multipart;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;

/**
 * Representation of form data.
 */
public final class FormData implements Iterable<String> {

    private static final Logger log = Logger.getLogger(FormData.class);

    private final Map<String, Deque<FormValue>> values = new LinkedHashMap<>();

    private final int maxValues;
    private int valueCount = 0;

    public FormData(final int maxValues) {
        this.maxValues = maxValues;
    }

    public Iterator<String> iterator() {
        return values.keySet().iterator();
    }

    public FormValue getFirst(String name) {
        final Deque<FormValue> deque = values.get(name);
        return deque == null ? null : deque.peekFirst();
    }

    public FormValue getLast(String name) {
        final Deque<FormValue> deque = values.get(name);
        return deque == null ? null : deque.peekLast();
    }

    public Deque<FormValue> get(String name) {
        return values.get(name);
    }

    public void add(String name, byte[] value, String fileName, CaseInsensitiveMap<String> headers) {
        Deque<FormValue> values = this.values.get(name);
        if (values == null) {
            this.values.put(name, values = new ArrayDeque<>(1));
        }
        values.add(new FormValueImpl(value, fileName, headers));
        if (++valueCount > maxValues) {
            throw new RuntimeException("Param limit of " + maxValues + " was exceeded");
        }
    }

    public void add(String name, String value) {
        add(name, value, null, null);
    }

    public void add(String name, String value, final CaseInsensitiveMap<String> headers) {
        add(name, value, null, headers);
    }

    public void add(String name, String value, String charset, final CaseInsensitiveMap<String> headers) {
        Deque<FormValue> values = this.values.get(name);
        if (values == null) {
            this.values.put(name, values = new ArrayDeque<>(1));
        }
        values.add(new FormValueImpl(value, charset, headers));
        if (++valueCount > maxValues) {
            throw new RuntimeException("Param limit of " + maxValues + " was exceeded");
        }
    }

    public void add(String name, Path value, String fileName, final CaseInsensitiveMap<String> headers) {
        Deque<FormValue> values = this.values.get(name);
        if (values == null) {
            this.values.put(name, values = new ArrayDeque<>(1));
        }
        values.add(new FormValueImpl(value, fileName, headers));
        if (values.size() > maxValues) {
            throw new RuntimeException("Param limit of " + maxValues + " was exceeded");
        }
        if (++valueCount > maxValues) {
            throw new RuntimeException("Param limit of " + maxValues + " was exceeded");
        }
    }

    public void put(String name, String value, final CaseInsensitiveMap<String> headers) {
        Deque<FormValue> values = new ArrayDeque<>(1);
        Deque<FormValue> old = this.values.put(name, values);
        if (old != null) {
            valueCount -= old.size();
        }
        values.add(new FormValueImpl(value, headers));

        if (++valueCount > maxValues) {
            throw new RuntimeException("Param limit of " + maxValues + " was exceeded");
        }
    }

    public Deque<FormValue> remove(String name) {
        Deque<FormValue> old = values.remove(name);
        if (old != null) {
            valueCount -= old.size();
        }
        return old;
    }

    public boolean contains(String name) {
        final Deque<FormValue> value = values.get(name);
        return value != null && !value.isEmpty();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final FormData strings = (FormData) o;

        if (values != null ? !values.equals(strings.values) : strings.values != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return values != null ? values.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "FormData{" +
                "values=" + values +
                '}';
    }

    public void deleteFiles() {
        for (Deque<FormValue> i : values.values()) {
            for (FormValue j : i) {
                if (j.isFileItem() && !j.getFileItem().isInMemory()) {
                    try {
                        Files.deleteIfExists(j.getFileItem().file);
                    } catch (IOException e) {
                        log.error("Cannot remove uploaded file " + j.getFileItem().file, e);
                    }
                }
            }
        }
    }

    public interface FormValue {

        /**
         * @return the simple string value.
         * @throws IllegalStateException If this is not a simple string value
         */
        String getValue();

        /**
         * @return The charset of the simple string value
         */
        String getCharset();

        /**
         * Returns true if this is a file and not a simple string
         *
         * @return
         */
        @Deprecated
        boolean isFile();

        /**
         * @return The temp file that the file data was saved to
         *
         * @throws IllegalStateException if this is not a file
         */
        @Deprecated
        Path getPath();

        @Deprecated
        File getFile();

        FileItem getFileItem();

        boolean isFileItem();

        /**
         * @return The filename specified in the disposition header.
         */
        String getFileName();

        /**
         * @return The headers that were present in the multipart request, or null if this was not a multipart request
         */
        CaseInsensitiveMap<String> getHeaders();
    }

    public static class FileItem {
        private final Path file;
        private final byte[] content;

        public FileItem(Path file) {
            this.file = file;
            this.content = null;
        }

        public FileItem(byte[] content) {
            this.file = null;
            this.content = content;
        }

        public boolean isInMemory() {
            return file == null;
        }

        public Path getFile() {
            return file;
        }

        public long getFileSize() throws IOException {
            if (isInMemory()) {
                return content.length;
            } else {
                return Files.size(file);
            }
        }

        public InputStream getInputStream() throws IOException {
            if (file != null) {
                return new BufferedInputStream(Files.newInputStream(file));
            } else {
                return new ByteArrayInputStream(content);
            }
        }

        public void delete() throws IOException {
            if (file != null) {
                try {
                    Files.delete(file);
                } catch (NoSuchFileException e) { //already deleted
                }
            }
        }

        public void write(Path target) throws IOException {
            if (file != null) {
                try {
                    Files.move(file, target);
                    return;
                } catch (IOException e) {
                    // ignore and let the Files.copy, outside
                    // this if block, take over and attempt to copy it
                }
            }
            try (InputStream is = getInputStream()) {
                Files.copy(is, target);
            }
        }
    }

    static class FormValueImpl implements FormValue {

        private final String value;
        private final String fileName;
        private final CaseInsensitiveMap<String> headers;
        private final FileItem fileItem;
        private final String charset;

        FormValueImpl(String value, CaseInsensitiveMap<String> headers) {
            this.value = value;
            this.headers = headers;
            this.fileName = null;
            this.fileItem = null;
            this.charset = null;
        }

        FormValueImpl(String value, String charset, CaseInsensitiveMap<String> headers) {
            this.value = value;
            this.charset = charset;
            this.headers = headers;
            this.fileName = null;
            this.fileItem = null;
        }

        FormValueImpl(Path file, final String fileName, CaseInsensitiveMap<String> headers) {
            this.fileItem = new FileItem(file);
            this.headers = headers;
            this.fileName = fileName;
            this.value = null;
            this.charset = null;
        }

        FormValueImpl(byte[] data, String fileName, CaseInsensitiveMap<String> headers) {
            this.fileItem = new FileItem(data);
            this.fileName = fileName;
            this.headers = headers;
            this.value = null;
            this.charset = null;
        }

        @Override
        public String getValue() {
            if (value == null) {
                throw new RuntimeException("Form value is a file");
            }
            return value;
        }

        @Override
        public String getCharset() {
            return charset;
        }

        @Override
        public boolean isFile() {
            return fileItem != null && !fileItem.isInMemory();
        }

        @Override
        public Path getPath() {
            if (fileItem == null) {
                throw new RuntimeException("Form value is a string");
            }
            if (fileItem.isInMemory()) {
                throw new RuntimeException("Form value is a memory file");
            }
            return fileItem.getFile();
        }

        @Override
        public File getFile() {
            return getPath().toFile();
        }

        @Override
        public FileItem getFileItem() {
            if (fileItem == null) {
                throw new RuntimeException("Form value is a string");
            }
            return fileItem;
        }

        @Override
        public boolean isFileItem() {
            return fileItem != null;
        }

        @Override
        public CaseInsensitiveMap<String> getHeaders() {
            return headers;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
