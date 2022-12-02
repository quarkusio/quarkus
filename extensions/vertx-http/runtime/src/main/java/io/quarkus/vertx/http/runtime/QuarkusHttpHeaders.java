package io.quarkus.vertx.http.runtime;

import static io.netty.handler.codec.http.HttpConstants.COLON;
import static io.netty.handler.codec.http.HttpConstants.CR;
import static io.netty.handler.codec.http.HttpConstants.LF;
import static io.netty.handler.codec.http.HttpConstants.SP;
import static io.netty.util.AsciiString.CASE_INSENSITIVE_HASHER;
import static io.netty.util.AsciiString.CASE_SENSITIVE_HASHER;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.HashingStrategy;
import io.vertx.core.MultiMap;
import io.vertx.core.http.impl.HttpUtils;

/**
 * Used to allow propagation of context objects from intra-JVM request producers
 */
public final class QuarkusHttpHeaders extends HttpHeaders implements MultiMap {

    private Map<Class<?>, Object> contextObjects;

    @Override
    public MultiMap setAll(MultiMap headers) {
        return set0(headers);
    }

    @Override
    public MultiMap setAll(Map<String, String> headers) {
        return set0(headers.entrySet());
    }

    @Override
    public int size() {
        return names().size();
    }

    private final QuarkusHttpHeaders.MapEntry[] entries = new QuarkusHttpHeaders.MapEntry[16];
    private final QuarkusHttpHeaders.MapEntry head = new QuarkusHttpHeaders.MapEntry();

    public QuarkusHttpHeaders() {
        head.before = head.after = head;
    }

    @Override
    public QuarkusHttpHeaders add(CharSequence name, CharSequence value) {
        Objects.requireNonNull(value);
        int h = AsciiString.hashCode(name);
        int i = h & 0x0000000F;
        add0(h, i, name, value);
        return this;
    }

    @Override
    public QuarkusHttpHeaders add(CharSequence name, Object value) {
        return add(name, (CharSequence) value);
    }

    @Override
    public HttpHeaders add(String name, Object value) {
        return add((CharSequence) name, (CharSequence) value);
    }

    @Override
    public QuarkusHttpHeaders add(String name, String strVal) {
        return add((CharSequence) name, strVal);
    }

    @Override
    public QuarkusHttpHeaders add(CharSequence name, Iterable values) {
        int h = AsciiString.hashCode(name);
        int i = h & 0x0000000F;
        for (Object vstr : values) {
            add0(h, i, name, (String) vstr);
        }
        return this;
    }

    @Override
    public QuarkusHttpHeaders add(String name, Iterable values) {
        return add((CharSequence) name, values);
    }

    @Override
    public MultiMap addAll(MultiMap headers) {
        return addAll(headers.entries());
    }

    @Override
    public MultiMap addAll(Map<String, String> map) {
        return addAll(map.entrySet());
    }

    private MultiMap addAll(Iterable<Map.Entry<String, String>> headers) {
        for (Map.Entry<String, String> entry : headers) {
            add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public QuarkusHttpHeaders remove(CharSequence name) {
        Objects.requireNonNull(name, "name");
        int h = AsciiString.hashCode(name);
        int i = h & 0x0000000F;
        remove0(h, i, name);
        return this;
    }

    @Override
    public QuarkusHttpHeaders remove(final String name) {
        return remove((CharSequence) name);
    }

    @Override
    public QuarkusHttpHeaders set(CharSequence name, CharSequence value) {
        return set0(name, value);
    }

    @Override
    public QuarkusHttpHeaders set(String name, String value) {
        return set((CharSequence) name, value);
    }

    @Override
    public QuarkusHttpHeaders set(String name, Object value) {
        return set((CharSequence) name, (CharSequence) value);
    }

    @Override
    public QuarkusHttpHeaders set(CharSequence name, Object value) {
        return set(name, (CharSequence) value);
    }

    @Override
    public QuarkusHttpHeaders set(CharSequence name, Iterable values) {
        Objects.requireNonNull(values, "values");

        int h = AsciiString.hashCode(name);
        int i = h & 0x0000000F;

        remove0(h, i, name);
        for (Object v : values) {
            if (v == null) {
                break;
            }
            add0(h, i, name, (CharSequence) v);
        }

        return this;
    }

    @Override
    public QuarkusHttpHeaders set(String name, Iterable values) {
        return set((CharSequence) name, values);
    }

    @Override
    public boolean contains(CharSequence name, CharSequence value, boolean ignoreCase) {
        int h = AsciiString.hashCode(name);
        int i = h & 0x0000000F;
        QuarkusHttpHeaders.MapEntry e = entries[i];
        HashingStrategy<CharSequence> strategy = ignoreCase ? CASE_INSENSITIVE_HASHER : CASE_SENSITIVE_HASHER;
        while (e != null) {
            CharSequence key = e.key;
            if (e.hash == h && (name == key || AsciiString.contentEqualsIgnoreCase(name, key))) {
                if (strategy.equals(value, e.getValue())) {
                    return true;
                }
            }
            e = e.next;
        }
        return false;
    }

    @Override
    public boolean contains(String name, String value, boolean ignoreCase) {
        return contains((CharSequence) name, value, ignoreCase);
    }

    @Override
    public boolean contains(CharSequence name) {
        return get0(name) != null;
    }

    @Override
    public boolean contains(String name) {
        return contains((CharSequence) name);
    }

    @Override
    public String get(CharSequence name) {
        Objects.requireNonNull(name, "name");
        CharSequence ret = get0(name);
        return ret != null ? ret.toString() : null;
    }

    @Override
    public String get(String name) {
        return get((CharSequence) name);
    }

    @Override
    public List<String> getAll(CharSequence name) {
        Objects.requireNonNull(name, "name");

        LinkedList<String> values = new LinkedList<>();

        int h = AsciiString.hashCode(name);
        int i = h & 0x0000000F;
        QuarkusHttpHeaders.MapEntry e = entries[i];
        while (e != null) {
            CharSequence key = e.key;
            if (e.hash == h && (name == key || AsciiString.contentEqualsIgnoreCase(name, key))) {
                values.addFirst(e.getValue().toString());
            }
            e = e.next;
        }
        return values;
    }

    @Override
    public List<String> getAll(String name) {
        return getAll((CharSequence) name);
    }

    @Override
    public void forEach(Consumer<? super Map.Entry<String, String>> action) {
        QuarkusHttpHeaders.MapEntry e = head.after;
        while (e != head) {
            action.accept(new AbstractMap.SimpleEntry<>(e.key.toString(), e.value.toString()));
            e = e.after;
        }
    }

    @Override
    public List<Map.Entry<String, String>> entries() {
        return MultiMap.super.entries();
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return new Iterator<Map.Entry<String, String>>() {
            MapEntry curr = head;

            @Override
            public boolean hasNext() {
                return curr.after != head;
            }

            @Override
            public Map.Entry<String, String> next() {
                MapEntry next = curr.after;
                if (next == head) {
                    throw new NoSuchElementException();
                }
                curr = next;
                return new Map.Entry<String, String>() {
                    @Override
                    public String getKey() {
                        return next.key.toString();
                    }

                    @Override
                    public String getValue() {
                        return next.value.toString();
                    }

                    @Override
                    public String setValue(String value) {
                        return next.setValue(value).toString();
                    }

                    @Override
                    public String toString() {
                        return getKey() + ": " + getValue();
                    }
                };
            }
        };
    }

    @Override
    public boolean isEmpty() {
        return head == head.after;
    }

    @Override
    public Set<String> names() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        QuarkusHttpHeaders.MapEntry e = head.after;
        while (e != head) {
            names.add(e.getKey().toString());
            e = e.after;
        }
        return names;
    }

    @Override
    public QuarkusHttpHeaders clear() {
        for (int i = 0; i < entries.length; i++) {
            entries[i] = null;
        }
        head.before = head.after = head;
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : this) {
            sb.append(entry).append('\n');
        }
        return sb.toString();
    }

    @Override
    public Integer getInt(CharSequence name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInt(CharSequence name, int defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Short getShort(CharSequence name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public short getShort(CharSequence name, short defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long getTimeMillis(CharSequence name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getTimeMillis(CharSequence name, long defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Map.Entry<CharSequence, CharSequence>> iteratorCharSequence() {
        return new Iterator<Map.Entry<CharSequence, CharSequence>>() {
            QuarkusHttpHeaders.MapEntry current = head.after;

            @Override
            public boolean hasNext() {
                return current != head;
            }

            @Override
            public Map.Entry<CharSequence, CharSequence> next() {
                Map.Entry<CharSequence, CharSequence> next = current;
                current = current.after;
                return next;
            }
        };
    }

    @Override
    public HttpHeaders addInt(CharSequence name, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders addShort(CharSequence name, short value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders setInt(CharSequence name, int value) {
        return set(name, Integer.toString(value));
    }

    @Override
    public HttpHeaders setShort(CharSequence name, short value) {
        throw new UnsupportedOperationException();
    }

    public void encode(ByteBuf buf) {
        QuarkusHttpHeaders.MapEntry current = head.after;
        while (current != head) {
            encoderHeader(current.key, current.value, buf);
            current = current.after;
        }
    }

    private static final int COLON_AND_SPACE_SHORT = (COLON << 8) | SP;
    static final int CRLF_SHORT = (CR << 8) | LF;

    static void encoderHeader(CharSequence name, CharSequence value, ByteBuf buf) {
        final int nameLen = name.length();
        final int valueLen = value.length();
        final int entryLen = nameLen + valueLen + 4;
        buf.ensureWritable(entryLen);
        int offset = buf.writerIndex();
        writeAscii(buf, offset, name);
        offset += nameLen;
        ByteBufUtil.setShortBE(buf, offset, COLON_AND_SPACE_SHORT);
        offset += 2;
        writeAscii(buf, offset, value);
        offset += valueLen;
        ByteBufUtil.setShortBE(buf, offset, CRLF_SHORT);
        offset += 2;
        buf.writerIndex(offset);
    }

    private static void writeAscii(ByteBuf buf, int offset, CharSequence value) {
        if (value instanceof AsciiString) {
            ByteBufUtil.copy((AsciiString) value, 0, buf, offset, value.length());
        } else {
            buf.setCharSequence(offset, value, CharsetUtil.US_ASCII);
        }
    }

    private static final class MapEntry implements Map.Entry<CharSequence, CharSequence> {
        final int hash;
        final CharSequence key;
        CharSequence value;
        QuarkusHttpHeaders.MapEntry next;
        QuarkusHttpHeaders.MapEntry before, after;

        MapEntry() {
            this.hash = -1;
            this.key = null;
            this.value = null;
        }

        MapEntry(int hash, CharSequence key, CharSequence value) {
            this.hash = hash;
            this.key = key;
            this.value = value;
        }

        void remove() {
            before.after = after;
            after.before = before;
        }

        void addBefore(QuarkusHttpHeaders.MapEntry e) {
            after = e;
            before = e.before;
            before.after = this;
            after.before = this;
        }

        @Override
        public CharSequence getKey() {
            return key;
        }

        @Override
        public CharSequence getValue() {
            return value;
        }

        @Override
        public CharSequence setValue(CharSequence value) {
            Objects.requireNonNull(value, "value");
            if (!io.vertx.core.http.HttpHeaders.DISABLE_HTTP_HEADERS_VALIDATION) {
                HttpUtils.validateHeaderValue(value);
            }
            CharSequence oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    private void remove0(int h, int i, CharSequence name) {
        QuarkusHttpHeaders.MapEntry e = entries[i];
        if (e == null) {
            return;
        }

        for (;;) {
            CharSequence key = e.key;
            if (e.hash == h && (name == key || AsciiString.contentEqualsIgnoreCase(name, key))) {
                e.remove();
                QuarkusHttpHeaders.MapEntry next = e.next;
                if (next != null) {
                    entries[i] = next;
                    e = next;
                } else {
                    entries[i] = null;
                    return;
                }
            } else {
                break;
            }
        }

        for (;;) {
            QuarkusHttpHeaders.MapEntry next = e.next;
            if (next == null) {
                break;
            }
            CharSequence key = next.key;
            if (next.hash == h && (name == key || AsciiString.contentEqualsIgnoreCase(name, key))) {
                e.next = next.next;
                next.remove();
            } else {
                e = next;
            }
        }
    }

    private void add0(int h, int i, final CharSequence name, final CharSequence value) {
        if (!io.vertx.core.http.HttpHeaders.DISABLE_HTTP_HEADERS_VALIDATION) {
            HttpUtils.validateHeader(name, value);
        }
        // Update the hash table.
        QuarkusHttpHeaders.MapEntry e = entries[i];
        QuarkusHttpHeaders.MapEntry newEntry;
        entries[i] = newEntry = new QuarkusHttpHeaders.MapEntry(h, name, value);
        newEntry.next = e;

        // Update the linked list.
        newEntry.addBefore(head);
    }

    private QuarkusHttpHeaders set0(final CharSequence name, final CharSequence strVal) {
        int h = AsciiString.hashCode(name);
        int i = h & 0x0000000F;
        remove0(h, i, name);
        if (strVal != null) {
            add0(h, i, name, strVal);
        }
        return this;
    }

    private CharSequence get0(CharSequence name) {
        int h = AsciiString.hashCode(name);
        int i = h & 0x0000000F;
        QuarkusHttpHeaders.MapEntry e = entries[i];
        CharSequence value = null;
        while (e != null) {
            CharSequence key = e.key;
            if (e.hash == h && (name == key || AsciiString.contentEqualsIgnoreCase(name, key))) {
                value = e.getValue();
            }
            e = e.next;
        }
        return value;
    }

    private MultiMap set0(Iterable<Map.Entry<String, String>> map) {
        clear();
        for (Map.Entry<String, String> entry : map) {
            add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public Map<Class<?>, Object> getContextObjects() {
        if (contextObjects == null) {
            return Collections.emptyMap();
        }
        return contextObjects;
    }

    public <T> QuarkusHttpHeaders setContextObject(Class<T> key, T type) {
        if (contextObjects == null) {
            contextObjects = new HashMap<>();
        }
        this.contextObjects.put(key, type);
        return this;
    }

    public <T> T getContextObject(Class<T> key) {
        return (T) getContextObjects().get(key);
    }
}