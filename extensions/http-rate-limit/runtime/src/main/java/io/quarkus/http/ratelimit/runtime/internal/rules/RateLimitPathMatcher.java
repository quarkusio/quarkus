package io.quarkus.http.ratelimit.runtime.internal.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.quarkus.vertx.http.runtime.security.ImmutablePathMatcher;

/**
 * Returns every registered value whose path pattern matches a request path.
 * <p>
 * Path syntax is that of {@code quarkus.http.auth.permission.*.paths} ({@link ImmutablePathMatcher}): exact, trailing
 * {@code /*}, inner {@code *} segment.
 * <p>
 * {@link ImmutablePathMatcher#match(String)} returns a single winner (exact, else longest prefix) and its handler
 * accumulator only merges values registered on the identical path string, so it cannot enumerate overlapping patterns.
 * This class therefore keeps one single-entry matcher per registered pattern and evaluates all of them; the number of
 * patterns is the number of configured rule paths, tens at most, so the cost is a short loop.
 */
public final class RateLimitPathMatcher<T> {

    private final List<Entry<T>> entries;

    private RateLimitPathMatcher(List<Entry<T>> entries) {
        this.entries = entries;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * @return every value whose pattern matches either path, in insertion order, deduplicated by identity; empty if
     *         none
     */
    public List<T> match(String path, String otherPath) {
        List<T> result = new ArrayList<>();
        for (Entry<T> entry : entries) {
            if ((entry.matcher.match(path).getValue() != null
                    || (!otherPath.equals(path) && entry.matcher.match(otherPath).getValue() != null))
                    && !containsIdentical(result, entry.value)) {
                result.add(entry.value);
            }
        }
        return result;
    }

    private static <T> boolean containsIdentical(List<T> values, T value) {
        for (T v : values) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

    private record Entry<T>(ImmutablePathMatcher<T> matcher, T value) {
    }

    public static final class Builder<T> {
        private final List<Entry<T>> entries = new ArrayList<>();

        private Builder() {
        }

        public Builder<T> add(String path, T value) {
            Objects.requireNonNull(value);
            ImmutablePathMatcher.ImmutablePathMatcherBuilder<T> builder = ImmutablePathMatcher.builder();
            builder.addPath(path, value);
            entries.add(new Entry<>(builder.build(), value));
            return this;
        }

        public RateLimitPathMatcher<T> build() {
            return new RateLimitPathMatcher<>(List.copyOf(entries));
        }
    }
}
