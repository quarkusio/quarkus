package io.quarkus.bootstrap.resolver.maven;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.eclipse.aether.version.Version;

public class BootstrapArtifactVersion implements Version {

    private final String version;

    private final Item[] items;

    private final int hash;

    /**
     * Creates a generic version from the specified string.
     *
     * @param version The version string, must not be {@code null}.
     */
    BootstrapArtifactVersion(String version) {
        this.version = version;
        items = parse(version);
        hash = Arrays.hashCode(items);
    }

    private static Item[] parse(String version) {
        List<Item> items = new ArrayList<>();

        for (Tokenizer tokenizer = new Tokenizer(version); tokenizer.next();) {
            Item item = tokenizer.toItem();
            items.add(item);
        }

        trimPadding(items);

        return items.toArray(new Item[items.size()]);
    }

    private static void trimPadding(List<Item> items) {
        Boolean number = null;
        int end = items.size() - 1;
        for (int i = end; i > 0; i--) {
            Item item = items.get(i);
            if (!Boolean.valueOf(item.isNumber()).equals(number)) {
                end = i;
                number = item.isNumber();
            }
            if (end == i && (i == items.size() - 1 || items.get(i - 1).isNumber() == item.isNumber())
                    && item.compareTo(null) == 0) {
                items.remove(i);
                end--;
            }
        }
    }

    public int compareTo(Version obj) {
        final Item[] these = items;
        final Item[] those = ((BootstrapArtifactVersion) obj).items;

        boolean number = true;

        for (int index = 0;; index++) {
            if (index >= these.length && index >= those.length) {
                return 0;
            } else if (index >= these.length) {
                return -comparePadding(those, index, null);
            } else if (index >= those.length) {
                return comparePadding(these, index, null);
            }

            Item thisItem = these[index];
            Item thatItem = those[index];

            if (thisItem.isNumber() != thatItem.isNumber()) {
                if (number == thisItem.isNumber()) {
                    return comparePadding(these, index, number);
                } else {
                    return -comparePadding(those, index, number);
                }
            } else {
                int rel = thisItem.compareTo(thatItem);
                if (rel != 0) {
                    return rel;
                }
                number = thisItem.isNumber();
            }
        }
    }

    private static int comparePadding(Item[] items, int index, Boolean number) {
        int rel = 0;
        for (int i = index; i < items.length; i++) {
            Item item = items[i];
            if (number != null && number != item.isNumber()) {
                break;
            }
            rel = item.compareTo(null);
            if (rel != 0) {
                break;
            }
        }
        return rel;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof BootstrapArtifactVersion) && compareTo((BootstrapArtifactVersion) obj) == 0;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return version;
    }

    static final class Tokenizer {

        private static final Integer QUALIFIER_ALPHA = -5;

        private static final Integer QUALIFIER_BETA = -4;

        private static final Integer QUALIFIER_MILESTONE = -3;

        private static final Map<String, Integer> QUALIFIERS;

        static {
            QUALIFIERS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            QUALIFIERS.put("alpha", QUALIFIER_ALPHA);
            QUALIFIERS.put("beta", QUALIFIER_BETA);
            QUALIFIERS.put("milestone", QUALIFIER_MILESTONE);
            QUALIFIERS.put("cr", -2);
            QUALIFIERS.put("rc", -2);
            QUALIFIERS.put("snapshot", -1);
            QUALIFIERS.put("ga", 0);
            QUALIFIERS.put("final", 0);
            QUALIFIERS.put("release", 0);
            QUALIFIERS.put("", 0);
            QUALIFIERS.put("sp", 1);
        }

        private final String version;

        private int index;

        private String token;

        private boolean number;

        private boolean terminatedByNumber;

        Tokenizer(String version) {
            this.version = (version.length() > 0) ? version : "0";
        }

        public boolean next() {
            final int n = version.length();
            if (index >= n) {
                return false;
            }

            int state = -2;

            int start = index;
            int end = n;
            terminatedByNumber = false;

            for (; index < n; index++) {
                char c = version.charAt(index);

                if (c == '.' || c == '-' || c == '_') {
                    end = index;
                    index++;
                    break;
                } else {
                    int digit = Character.digit(c, 10);
                    if (digit >= 0) {
                        if (state == -1) {
                            end = index;
                            terminatedByNumber = true;
                            break;
                        }
                        if (state == 0) {
                            // normalize numbers and strip leading zeros (prereq for Integer/BigInteger handling)
                            start++;
                        }
                        state = (state > 0 || digit > 0) ? 1 : 0;
                    } else {
                        if (state >= 0) {
                            end = index;
                            break;
                        }
                        state = -1;
                    }
                }

            }

            if (end - start > 0) {
                token = version.substring(start, end);
                number = state >= 0;
            } else {
                token = "0";
                number = true;
            }

            return true;
        }

        @Override
        public String toString() {
            return String.valueOf(token);
        }

        public Item toItem() {
            if (number) {
                try {
                    if (token.length() < 10) {
                        return new Item(Item.KIND_INT, Integer.parseInt(token));
                    } else {
                        return new Item(Item.KIND_BIGINT, new BigInteger(token));
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                if (index >= version.length()) {
                    if ("min".equalsIgnoreCase(token)) {
                        return Item.MIN;
                    } else if ("max".equalsIgnoreCase(token)) {
                        return Item.MAX;
                    }
                }
                if (terminatedByNumber && token.length() == 1) {
                    switch (token.charAt(0)) {
                        case 'a':
                        case 'A':
                            return new Item(Item.KIND_QUALIFIER, QUALIFIER_ALPHA);
                        case 'b':
                        case 'B':
                            return new Item(Item.KIND_QUALIFIER, QUALIFIER_BETA);
                        case 'm':
                        case 'M':
                            return new Item(Item.KIND_QUALIFIER, QUALIFIER_MILESTONE);
                        default:
                    }
                }
                Integer qualifier = QUALIFIERS.get(token);
                if (qualifier != null) {
                    return new Item(Item.KIND_QUALIFIER, qualifier);
                } else {
                    return new Item(Item.KIND_STRING, token.toLowerCase(Locale.ENGLISH));
                }
            }
        }

    }

    static final class Item {

        static final int KIND_MAX = 8;

        static final int KIND_BIGINT = 5;

        static final int KIND_INT = 4;

        static final int KIND_STRING = 3;

        static final int KIND_QUALIFIER = 2;

        static final int KIND_MIN = 0;

        static final Item MAX = new Item(KIND_MAX, "max");

        static final Item MIN = new Item(KIND_MIN, "min");

        private final int kind;

        private final Object value;

        Item(int kind, Object value) {
            this.kind = kind;
            this.value = value;
        }

        public boolean isNumber() {
            return (kind & KIND_QUALIFIER) == 0; // i.e. kind != string/qualifier
        }

        public int compareTo(Item that) {
            int rel;
            if (that == null) {
                // null in this context denotes the pad item (0 or "ga")
                switch (kind) {
                    case KIND_MIN:
                        rel = -1;
                        break;
                    case KIND_MAX:
                    case KIND_BIGINT:
                    case KIND_STRING:
                        rel = 1;
                        break;
                    case KIND_INT:
                    case KIND_QUALIFIER:
                        rel = (Integer) value;
                        break;
                    default:
                        throw new IllegalStateException("unknown version item kind " + kind);
                }
            } else {
                rel = kind - that.kind;
                if (rel == 0) {
                    switch (kind) {
                        case KIND_MAX:
                        case KIND_MIN:
                            break;
                        case KIND_BIGINT:
                            rel = ((BigInteger) value).compareTo((BigInteger) that.value);
                            break;
                        case KIND_INT:
                        case KIND_QUALIFIER:
                            rel = ((Integer) value).compareTo((Integer) that.value);
                            break;
                        case KIND_STRING:
                            rel = ((String) value).compareToIgnoreCase((String) that.value);
                            break;
                        default:
                            throw new IllegalStateException("unknown version item kind " + kind);
                    }
                }
            }
            return rel;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Item) && compareTo((Item) obj) == 0;
        }

        @Override
        public int hashCode() {
            return value.hashCode() + kind * 31;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

    }
}
