package org.jboss.shamrock.deployment.configuration;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TreeMap;

import org.jboss.shamrock.runtime.configuration.NameIterator;
import org.wildfly.common.Assert;

/**
 * A pattern-matching mapping of configuration key pattern to value.
 */
public final class ConfigPatternMap<T> implements Iterable<T> {
    public static final String WILD_CARD = "{*}";
    private T matched;
    private final TreeMap<String, ConfigPatternMap<T>> children = new TreeMap<>();

    /**
     * Find the item which matches the given configuration key.
     *
     * @param name the configuration key (must not be {@code null})
     * @return the matching item
     */
    public T match(String name) {
        Assert.checkNotNullParam("name", name);
        return match(new NameIterator(name, false));
    }

    /**
     * Find the item which matches the given configuration key iterator.
     *
     * @param nameIterator the configuration key iterator (must not be {@code null})
     * @return the matching item
     */
    public T match(NameIterator nameIterator) {
        Assert.checkNotNullParam("nameIterator", nameIterator);
        if (! nameIterator.hasNext()) return matched;
        nameIterator.next();
        try {
            return matchLeaf(nameIterator);
        } finally {
            nameIterator.previous();
        }
    }

    T matchLeaf(NameIterator nameIterator) {
        // current iterator position contains the child string to find
        ConfigPatternMap<T> next;
        next = children.get(nameIterator.getPreviousSegment());
        if (next != null) {
            return next.match(nameIterator);
        }
        next = children.get(WILD_CARD);
        if (next != null) {
            return next.match(nameIterator);
        }
        return null;
    }

    /**
     * Add a pattern to the map.
     * The segments of the pattern are dot-separated.
     * The special segment name {@code {*}} will match any single segment name.
     * The special segment name {@code {**}} will eagerly match any segment name sequence.
     *
     * @param pattern the pattern (must not be {@code null})
     * @param onMatch the value to return when the pattern is matched (must not be {@code null})
     * @return {@code true} if the pattern is a new unique pattern, or if the pattern exists but the given value
     *   is equal to the existing value; {@code false} if the pattern exists but the given value is not equal to the
     *   existing value
     */
    public boolean addPattern(String pattern, T onMatch) {
        Assert.checkNotNullParam("pattern", pattern);
        Assert.checkNotNullParam("onMatch", onMatch);
        return addPattern(new NameIterator(pattern, false), onMatch);
    }

    boolean addPattern(NameIterator nameIterator, T onMatch) {
        if (! nameIterator.hasNext()) {
            if (matched != null) {
                return Objects.equals(onMatch, matched);
            }
            matched = onMatch;
            return true;
        } else {
            nameIterator.next();
            try {
                return children.computeIfAbsent(getKey(nameIterator), s -> new ConfigPatternMap<>()).addPattern(nameIterator, onMatch);
            } finally {
                nameIterator.previous();
            }
        }
    }

    private static String getKey(final NameIterator nameIterator) {
        final String str = nameIterator.getPreviousSegment();
        if (str.equals(WILD_CARD)) return WILD_CARD;
        return str;
    }

    public T getMatched() {
        return matched;
    }

    public void setMatched(final T matched) {
        this.matched = matched;
    }

    public Iterable<String> childNames() {
        return () -> children.keySet().iterator();
    }

    public ConfigPatternMap<T> getChild(String name) {
        return children.get(name);
    }

    public PatternIterator<T> iterator() {
        return new PatternIterator<T>(children, this);
    }

    public void addChild(final String childName, final ConfigPatternMap<T> child) {
        children.put(childName, child);
    }

    public static class PatternIterator<T> implements Iterator<T> {
        ConfigPatternMap<T> current;
        ConfigPatternMap<T> next;
        PatternIterator<T> currentItr;
        Iterator<ConfigPatternMap<T>> childMaps;

        PatternIterator(final TreeMap<String, ConfigPatternMap<T>> children, final ConfigPatternMap<T> initial) {
            next = initial.getMatched() == null ? null : initial;
            childMaps = children.values().iterator();
        }

        public boolean hasNext() {
            while (next == null) {
                while (currentItr == null) {
                    if (! childMaps.hasNext()) {
                        return false;
                    }
                    final ConfigPatternMap<T> nextChildMap = childMaps.next();
                    currentItr = nextChildMap.iterator();
                }
                if (currentItr.hasNext()) {
                    next = currentItr.nextPattern();
                    if (next.getMatched() == null) {
                        next = null;
                    }
                } else {
                    currentItr = null;
                }
            }
            return true;
        }

        public T next() {
            return nextPattern().getMatched();
        }

        ConfigPatternMap<T> nextPattern() {
            if (! hasNext()) throw new NoSuchElementException();
            try {
                return current = next;
            } finally {
                next = null;
            }
        }

        public T current() {
            return currentPatternMap().getMatched();
        }

        public ConfigPatternMap<T> currentPatternMap() {
            final ConfigPatternMap<T> current = this.current;
            if (current == null) throw new NoSuchElementException();
            return current;
        }
    }
}
