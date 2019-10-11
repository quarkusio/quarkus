package io.quarkus.vertx.http.runtime.security;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A string keyed map that can be accessed as a substring, eliminating the need to allocate a new string
 * to do a key comparison against.
 * <p>
 * This class uses linear probing and is thread safe due to copy on write semantics. As such it is not recomended
 * for data that changes frequently.
 * <p>
 * This class does not actually implement the map interface to avoid implementing unnecessary operations.
 *
 * @author Stuart Douglas
 */
public class SubstringMap<V> {
    private static final int ALL_BUT_LAST_BIT = ~1;

    private volatile Object[] table = new Object[16];
    private int size;

    public SubstringMatch<V> get(String key, int length) {
        return get(key, length, false);
    }

    public SubstringMatch<V> get(String key) {
        return get(key, key.length(), false);
    }

    private SubstringMatch<V> get(String key, int length, boolean exact) {
        if (key.length() < length) {
            throw new IllegalArgumentException();
        }
        Object[] table = this.table;
        int hash = hash(key, length);
        int pos = tablePos(table, hash);
        int start = pos;
        while (table[pos] != null) {
            if (exact) {
                if (table[pos].equals(key)) {
                    return (SubstringMatch<V>) table[pos + 1];
                }
            } else {
                if (doEquals((String) table[pos], key, length)) {
                    return (SubstringMatch<V>) table[pos + 1];
                }
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

    private int tablePos(Object[] table, int hash) {
        return (hash & (table.length - 1)) & ALL_BUT_LAST_BIT;
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

    public synchronized void put(String key, V value) {
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

    public synchronized V remove(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        //we just assume it is present, and always do a copy
        //for this maps intended use cases as a path matcher it won't be called when
        //the value is not present anyway
        V value = null;
        Object[] newTable = new Object[table.length];
        for (int i = 0; i < table.length; i += 2) {
            if (table[i] != null && !table[i].equals(key)) {
                doPut(newTable, (String) table[i], table[i + 1]);
            } else if (table[i] != null) {
                value = (V) table[i + 1];
                size--;
            }
        }
        this.table = newTable;
        if (value == null) {
            return null;
        }
        return ((SubstringMatch<V>) value).getValue();
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

    public Map<String, V> toMap() {
        Map<String, V> map = new HashMap<>();
        Object[] t = this.table;
        for (int i = 0; i < t.length; i += 2) {
            if (t[i] != null) {
                map.put((String) t[i], ((SubstringMatch<V>) t[i + 1]).value);
            }
        }
        return map;
    }

    public synchronized void clear() {
        size = 0;
        table = new Object[16];
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

    public Iterable<String> keys() {
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
    }
}
