package org.jboss.resteasy.reactive.server.core.multipart;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;
import org.jboss.resteasy.reactive.server.multipart.FileItem;
import org.jboss.resteasy.reactive.server.multipart.FormValue;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;

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

    public MultipartFormDataInput toMultipartFormDataInput() {
        return new MultipartFormDataInput() {
            @SuppressWarnings("unchecked")
            @Override
            public Map<String, Collection<FormValue>> getValues() {
                Map<String, ? extends Collection<FormValue>> result = new LinkedHashMap<>(values);
                return (Map<String, Collection<FormValue>>) result;
            }
        };
    }

    @Override
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
                        Files.deleteIfExists(j.getFileItem().getFile());
                    } catch (IOException e) {
                        log.error("Cannot remove uploaded file " + j.getFileItem().getFile(), e);
                    }
                }
            }
        }
    }

    public static class FileItemImpl implements FileItem {
        private final Path file;
        private final byte[] content;

        public FileItemImpl(Path file) {
            this.file = file;
            this.content = null;
        }

        public FileItemImpl(byte[] content) {
            this.file = null;
            this.content = content;
        }

        @Override
        public boolean isInMemory() {
            return file == null;
        }

        @Override
        public Path getFile() {
            return file;
        }

        @Override
        public long getFileSize() throws IOException {
            if (isInMemory()) {
                return content.length;
            } else {
                return Files.size(file);
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (file != null) {
                return new BufferedInputStream(Files.newInputStream(file));
            } else {
                return new ByteArrayInputStream(content);
            }
        }

        @Override
        public void delete() throws IOException {
            if (file != null) {
                try {
                    Files.delete(file);
                } catch (NoSuchFileException e) { //already deleted
                }
            }
        }

        @Override
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
        private final FileItemImpl fileItemImpl;
        private final String charset;

        FormValueImpl(String value, CaseInsensitiveMap<String> headers) {
            this.value = value;
            this.headers = headers;
            this.fileName = null;
            this.fileItemImpl = null;
            this.charset = null;
        }

        FormValueImpl(String value, String charset, CaseInsensitiveMap<String> headers) {
            this.value = value;
            this.charset = charset;
            this.headers = headers;
            this.fileName = null;
            this.fileItemImpl = null;
        }

        FormValueImpl(Path file, final String fileName, CaseInsensitiveMap<String> headers) {
            this.fileItemImpl = new FileItemImpl(file);
            this.headers = headers;
            this.fileName = fileName;
            this.value = null;
            this.charset = null;
        }

        FormValueImpl(byte[] data, String fileName, CaseInsensitiveMap<String> headers) {
            this.fileItemImpl = new FileItemImpl(data);
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
        public FileItemImpl getFileItem() {
            if (fileItemImpl == null) {
                throw new RuntimeException("Form value is a string");
            }
            return fileItemImpl;
        }

        @Override
        public boolean isFileItem() {
            return fileItemImpl != null;
        }

        @Override
        public CaseInsensitiveMap<String> getHeaders() {
            return headers;
        }

        @Override
        public String getFileName() {
            return fileName;
        }
    }
}
