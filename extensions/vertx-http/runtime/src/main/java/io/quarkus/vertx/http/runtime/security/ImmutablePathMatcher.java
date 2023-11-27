package io.quarkus.vertx.http.runtime.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.runtime.security.ImmutableSubstringMap.SubstringMatch;

/**
 * Handler that dispatches to a given handler based on a match of the path.
 */
public class ImmutablePathMatcher<T> {

    private final ImmutableSubstringMap<T> paths;
    private final Map<String, T> exactPathMatches;

    /**
     * lengths of all registered paths
     */
    private final int[] lengths;
    private final T defaultHandler;
    private final boolean hasPathWithInnerWildcard;
    private final boolean hasExactPathMatches;

    private ImmutablePathMatcher(T defaultHandler, ImmutableSubstringMap<T> paths, Map<String, T> exactPathMatches,
            int[] lengths, boolean hasPathWithInnerWildcard) {
        this.defaultHandler = defaultHandler;
        this.paths = paths;
        this.lengths = Arrays.copyOf(lengths, lengths.length);
        this.hasPathWithInnerWildcard = hasPathWithInnerWildcard;
        if (exactPathMatches.isEmpty()) {
            this.exactPathMatches = null;
            this.hasExactPathMatches = false;
        } else {
            this.exactPathMatches = Map.copyOf(exactPathMatches);
            this.hasExactPathMatches = true;
        }
    }

    /**
     * Matches a path against the registered handlers.
     *
     * @param path The relative path to match
     * @return The match. This will never be null, however if none matched its value field will be
     */
    public PathMatch<T> match(String path) {
        if (hasExactPathMatches) {
            T match = exactPathMatches.get(path);
            if (match != null) {
                return new PathMatch<>(path, "", match);
            }
        }

        int length = path.length();
        for (int pathLength : lengths) {
            if (pathLength == length) {
                SubstringMatch<T> next = paths.get(path, length);
                if (next != null) {
                    return new PathMatch<>(path, "", next.getValue());
                }
            } else if (pathLength < length) {
                char c = path.charAt(pathLength);
                // pathLength == 1 means prefix path is / because prefix path always starts with /
                // which means it's default handler match, but if there is at least
                // one path with inner wildcard, we need to check for paths like /*/one
                if (c == '/' || (hasPathWithInnerWildcard && pathLength == 1)) {

                    //String part = path.substring(0, pathLength);
                    SubstringMatch<T> next = paths.get(path, pathLength);
                    if (next != null) {
                        return new PathMatch<>(next.getKey(), path.substring(pathLength), next.getValue());
                    }
                }
            }
        }
        return new PathMatch<>("", path, defaultHandler);
    }

    public static <T> ImmutablePathMatcherBuilder<T> builder() {
        return new ImmutablePathMatcherBuilder<>();
    }

    public static final class PathMatch<T> {
        private final String matched;
        private final String remaining;
        private final T value;

        public PathMatch(String matched, String remaining, T value) {
            this.matched = matched;
            this.remaining = remaining;
            this.value = value;
        }

        /**
         * @deprecated because it can't be supported with inner wildcard without cost. It's unlikely this method is
         *             used by anyone as users don't get in touch with this class. If there is legit use case, please
         *             open Quarkus issue.
         */
        @Deprecated
        public String getRemaining() {
            return remaining;
        }

        public String getMatched() {
            return matched;
        }

        public T getValue() {
            return value;
        }
    }

    public static class ImmutablePathMatcherBuilder<T> {

        private static final String STRING_PATH_SEPARATOR = "/";
        private final Map<String, T> exactPathMatches = new HashMap<>();
        private final Map<String, Path<T>> pathsWithWildcard = new HashMap<>();
        private BiConsumer<T, T> handlerAccumulator;

        private ImmutablePathMatcherBuilder() {
        }

        /**
         * @param handlerAccumulator policies defined with same path are accumulated, this way, you can define
         *        more than one policy of one path (e.g. one for POST method, one for GET method)
         * @return ImmutablePathMatcherBuilder
         */
        public ImmutablePathMatcherBuilder<T> handlerAccumulator(BiConsumer<T, T> handlerAccumulator) {
            this.handlerAccumulator = handlerAccumulator;
            return this;
        }

        public ImmutablePathMatcher<T> build() {
            T defaultHandler = null;
            SubstringMap<T> paths = new SubstringMap<>();
            boolean hasPathWithInnerWildcard = false;
            // process paths with a wildcard first, that way we only create inner path matcher when really needed
            for (Path<T> p : pathsWithWildcard.values()) {
                T handler = null;
                ImmutablePathMatcher<SubstringMatch<T>> subPathMatcher = null;

                if (p.prefixPathHandler != null) {
                    handler = p.prefixPathHandler;
                    if (STRING_PATH_SEPARATOR.equals(p.path)) {
                        if (defaultHandler == null) {
                            defaultHandler = p.prefixPathHandler;
                        } else {
                            handlerAccumulator.accept(defaultHandler, p.prefixPathHandler);
                        }
                    }
                }

                if (p.pathsWithInnerWildcard != null) {
                    if (!hasPathWithInnerWildcard) {
                        hasPathWithInnerWildcard = true;
                    }
                    // create path matcher for sub-path after inner wildcard: /one/*/three/four => /three/four
                    var builder = new ImmutablePathMatcherBuilder<SubstringMatch<T>>();
                    if (handlerAccumulator != null) {
                        builder.handlerAccumulator(
                                new BiConsumer<SubstringMatch<T>, SubstringMatch<T>>() {
                                    @Override
                                    public void accept(SubstringMatch<T> match1, SubstringMatch<T> match2) {
                                        if (match2.hasSubPathMatcher()) {
                                            // this should be impossible to happen since these matches are created
                                            // right in this 'build()' method, but let's make sure of that
                                            throw new IllegalStateException(
                                                    String.format("Failed to merge sub-matches with key '%s' for path '%s'",
                                                            match1.getKey(), p.originalPath));
                                        }
                                        handlerAccumulator.accept(match1.getValue(), match2.getValue());
                                    }
                                });
                    }
                    for (PathWithInnerWildcard<T> p1 : p.pathsWithInnerWildcard) {
                        builder.addPath(p.originalPath, p1.remaining, new SubstringMatch<>(p1.remaining, p1.handler));
                    }
                    subPathMatcher = builder.build();
                }

                paths.put(p.path, handler, subPathMatcher);
            }
            int[] lengths = buildLengths(paths.keys());
            return new ImmutablePathMatcher<>(defaultHandler, paths.asImmutableMap(), exactPathMatches, lengths,
                    hasPathWithInnerWildcard);
        }

        /**
         * Two sorts of paths are accepted:
         * - exact path matches (without wildcard); these are matched first and Quarkus does no magic,
         * request path must exactly match
         * - paths with one or more wildcard:
         * - ending wildcard matches zero or more path segment
         * - inner wildcard matches exactly one path segment
         * few notes:
         * - it's key to understand only segments are matched, for example '/one*' will not match request path '/ones'
         * - path patterns '/one*' and '/one/*' are one and the same thing as we only match path segments and '/one*'
         * in fact means 'either /one or /one/any-number-of-path-segments'
         * - paths are matched on longer-prefix-wins basis
         * - what we call 'prefix' is in fact path to the first wildcard
         * - if there is a path after first wildcard like in the '/one/*\/three' pattern ('/three' is remainder)
         * path pattern is considered longer than the '/one/*' pattern and wins for request path '/one/two/three'
         * - more specific pattern wins and wildcard is always less specific than any other path segment character,
         * therefore path '/one/two/three*' will win over '/one/*\/three*' for request path '/one/two/three/four'
         *
         * @param path normalized path
         * @param handler prefix path handler
         * @return self
         */
        public ImmutablePathMatcherBuilder<T> addPath(String path, T handler) {
            return addPath(path, path, handler);
        }

        private ImmutablePathMatcherBuilder<T> addPath(String originalPath, String path, T handler) {
            if (!path.startsWith("/")) {
                String errMsg = "Path must always start with a path separator, but was '" + path + "'";
                if (!originalPath.equals(path)) {
                    errMsg += " created from original path pattern '" + originalPath + "'";
                }
                throw new IllegalArgumentException(errMsg);
            }
            final int wildcardIdx = path.indexOf('*');
            if (wildcardIdx == -1) {
                addExactPath(path, handler);
            } else {
                addWildcardPath(path, handler, wildcardIdx, originalPath);
            }
            return this;
        }

        private void addWildcardPath(String path, T handler, int wildcardIdx, String originalPath) {
            final int lastIdx = path.length() - 1;
            final String pathWithWildcard;
            final String pathAfter1stWildcard;

            if (lastIdx == wildcardIdx) {
                // ends with a wildcard => it's a prefix path
                pathWithWildcard = path;
                pathAfter1stWildcard = null;
            } else {
                // contains at least one inner wildcard: /one/*/three, /one/two/*/four/*, ...
                // the inner wildcard represents exactly one path segment
                pathWithWildcard = path.substring(0, wildcardIdx + 1);
                pathAfter1stWildcard = path.substring(wildcardIdx + 1);

                // validate that inner wildcard is enclosed with path separators like: /one/*/two
                // anything like: /one*/two, /one/*two/, /one/tw*o/ is not allowed
                if (!pathWithWildcard.endsWith("/*") || !pathAfter1stWildcard.startsWith("/")) {
                    throw new ConfigurationException("HTTP permission path '" + originalPath + "' contains inner "
                            + "wildcard enclosed with a path character other than a separator. The inner wildcard "
                            + "must represent exactly one path segment. Please see this Quarkus guide for more "
                            + "information: https://quarkus.io/guides/security-authorize-web-endpoints-reference");
                }
            }

            final String pathWithoutWildcard;
            if (pathWithWildcard.endsWith("/*")) {
                // remove /*
                String stripped = pathWithWildcard.substring(0, pathWithWildcard.length() - 2);
                pathWithoutWildcard = stripped.isEmpty() ? "/" : stripped;
            } else {
                // remove *
                pathWithoutWildcard = pathWithWildcard.substring(0, pathWithWildcard.length() - 1);
            }

            Path<T> p = pathsWithWildcard.computeIfAbsent(pathWithoutWildcard, Path::new);
            p.originalPath = originalPath;
            if (pathAfter1stWildcard == null) {
                p.addPrefixPath(handler, handlerAccumulator);
            } else {
                p.addPathWithInnerWildcard(pathAfter1stWildcard, handler);
            }
        }

        private void addExactPath(final String path, final T handler) {
            if (path.isEmpty()) {
                throw new IllegalArgumentException("Path not specified");
            }
            if (exactPathMatches.containsKey(path) && handlerAccumulator != null) {
                handlerAccumulator.accept(exactPathMatches.get(path), handler);
            } else {
                exactPathMatches.put(path, handler);
            }
        }

        private static int[] buildLengths(Iterable<String> keys) {
            final Set<Integer> lengths = new TreeSet<>(new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return -o1.compareTo(o2);
                }
            });
            for (String p : keys) {
                lengths.add(p.length());
            }

            int[] lengthArray = new int[lengths.size()];
            int pos = 0;
            for (int i : lengths) {
                lengthArray[pos++] = i;
            }
            return lengthArray;
        }
    }

    private static class Path<T> {
        private final String path;
        private String originalPath = null;
        private T prefixPathHandler = null;
        private List<PathWithInnerWildcard<T>> pathsWithInnerWildcard = null;

        private Path(String path) {
            this.path = path;
        }

        private void addPathWithInnerWildcard(String remaining, T handler) {
            if (pathsWithInnerWildcard == null) {
                pathsWithInnerWildcard = new ArrayList<>();
            }
            pathsWithInnerWildcard.add(new PathWithInnerWildcard<>(remaining, handler));
        }

        public void addPrefixPath(T prefixPathHandler, BiConsumer<T, T> handlerAccumulator) {
            Objects.requireNonNull(prefixPathHandler);
            if (this.prefixPathHandler != null && handlerAccumulator != null) {
                handlerAccumulator.accept(this.prefixPathHandler, prefixPathHandler);
            } else {
                this.prefixPathHandler = prefixPathHandler;
            }
        }
    }

    private static class PathWithInnerWildcard<T> {
        private final String remaining;
        private final T handler;

        private PathWithInnerWildcard(String remaining, T handler) {
            this.remaining = remaining;
            this.handler = handler;
        }
    }
}
