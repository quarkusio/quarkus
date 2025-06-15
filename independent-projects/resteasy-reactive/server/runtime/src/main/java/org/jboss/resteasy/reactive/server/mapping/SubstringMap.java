package org.jboss.resteasy.reactive.server.mapping;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A string keyed map that can be accessed as a substring, eliminating the need to allocate a new string to do a key
 * comparison against.
 * <p>
 * This class uses linear probing and is thread safe due to copy on write semantics. As such it is not recomended for
 * data that changes frequently.
 * <p>
 * This class does not actually implement the map interface to avoid implementing unnecessary operations.
 *
 * @author Stuart Douglas
 */
class SubstringMap<V> {
    private static final int ALL_BUT_LAST_BIT = ~1;

    private final Object[] table;
    private final int size;

    public SubstringMap(Object[] table, int size) {
        this.table = table;
        this.size = size;
    }

    int size() {
        return size;
    }

    SubstringMatch<V> get(String key, int length) {
        return doGet(key, length);
    }

    SubstringMatch<V> get(String key) {
        return doGet(key, key.length());
    }

    private SubstringMatch<V> doGet(String key, int length) {
        if (key.length() < length) {
            throw new IllegalArgumentException();
        }
        Object[] table = this.table;
        int hash = hash(key, length);
        int pos = tablePos(table, hash);
        int start = pos;
        while (table[pos] != null) {
            if (doEquals((String) table[pos], key, length)) {
                return (SubstringMatch<V>) table[pos + 1];
            }
            pos += 2;
            if (pos >= table.length) {
                pos = 0;
            }
            if (pos == start) {
                return null;
            }
        }
        return null;
    }

    private static int tablePos(Object[] table, int hash) {
        return (hash & (table.length - 1)) & ALL_BUT_LAST_BIT;
    }

    private static int hash(String value, int length) {
        if (length == 0) {
            return 0;
        }
        int h = 0;
        for (int i = 0; i < length; i++) {
            h = 31 * h + value.charAt(i);
        }
        return h;
    }

    private boolean doEquals(String s1, String s2, int length) {
        if (s1.length() != length || s2.length() < length) {
            return false;
        }
        for (int i = 0; i < length; ++i) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    Iterable<String> keys() {
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                final Object[] tMap = table;
                int i = 0;
                while (i < table.length && tMap[i] == null) {
                    i += 2;
                }
                final int startPos = i;

                return new Iterator<String>() {

                    private Object[] map = tMap;

                    private int pos = startPos;

                    @Override
                    public boolean hasNext() {
                        return pos < table.length;
                    }

                    @Override
                    public String next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        String ret = (String) map[pos];

                        pos += 2;
                        while (pos < table.length && tMap[pos] == null) {
                            pos += 2;
                        }
                        return ret;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };

    }

    static class Builder<V> {

        private Object[] table = new Object[16];
        private int size;

        SubstringMap<V> build() {
            return new SubstringMap<>(table, size);
        }

        void put(String key, V value) {
            if (key == null) {
                throw new NullPointerException();
            }
            Object[] newTable;
            if (table.length / (double) size < 4 && table.length != Integer.MAX_VALUE) {
                newTable = new Object[table.length << 1];
                for (int i = 0; i < table.length; i += 2) {
                    if (table[i] != null) {
                        doPut(newTable, (String) table[i], table[i + 1]);
                    }
                }
            } else {
                newTable = new Object[table.length];
                System.arraycopy(table, 0, newTable, 0, table.length);
            }
            doPut(newTable, key, new SubstringMap.SubstringMatch<>(key, value));
            this.table = newTable;
            size++;
        }

        private void doPut(Object[] newTable, String key, Object value) {
            int hash = hash(key, key.length());
            int pos = tablePos(newTable, hash);
            while (newTable[pos] != null && !newTable[pos].equals(key)) {
                pos += 2;
                if (pos >= newTable.length) {
                    pos = 0;
                }
            }
            newTable[pos] = key;
            newTable[pos + 1] = value;
        }
    }

    public static final class SubstringMatch<V> {
        private final String key;
        private final V value;

        public SubstringMatch(String key, V value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "SubstringMatch{ key: " + key + ", value: " + value + " }";
        }
    }
}
