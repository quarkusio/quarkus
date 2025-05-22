package org.jboss.resteasy.reactive.server.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;

public class RequestMapper<T> {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final PathMatcher<ArrayList<RequestPath<T>>> requestPaths;
    private final PathMatcher.Builder<ArrayList<RequestPath<T>>> pathMatcherBuilder;
    private final ArrayList<RequestPath<T>> templates;
    final int maxParams;

    public RequestMapper(ArrayList<RequestPath<T>> templates) {
        pathMatcherBuilder = new PathMatcher.Builder<>();
        this.templates = templates;
        int max = 0;
        Map<String, ArrayList<RequestPath<T>>> aggregates = new HashMap<>();
        for (RequestPath<T> i : templates) {
            ArrayList<RequestPath<T>> paths = aggregates.get(i.template.stem);
            if (paths == null) {
                aggregates.put(i.template.stem, paths = new ArrayList<>());
            }
            paths.add(i);
            max = Math.max(max, i.template.countPathParamNames());
        }
        aggregates.forEach(new BiConsumer<>() {
            @Override
            public void accept(String stem, ArrayList<RequestPath<T>> list) {
                Collections.sort(list);
                pathMatcherBuilder.addPrefixPath(stem, list);
            }
        });
        maxParams = max;
        requestPaths = pathMatcherBuilder.build();
    }

    /**
     * Match the path to the UriTemplates. Returns the best match, meaning the least remaining path after match.
     *
     * @param path path to search UriTemplate for
     * @return best RequestMatch, or null if the path has no match
     */
    public RequestMatch<T> map(String path) {
        var result = mapFromPathMatcher(path, requestPaths.match(path), 0);
        if (result != null) {
            return result;
        }

        // the following code is meant to handle cases like https://github.com/quarkusio/quarkus/issues/30667
        return mapFromPathMatcher(path, requestPaths.defaultMatch(path), 0);
    }

    /**
     * Continue matching for the next best path starting from the last match, meaning the least remaining path after match.
     *
     * @param path path to search UriTemplate for
     * @return another RequestMatch. Might return null if all matches are exhausted.
     */
    public RequestMatch<T> continueMatching(String path, RequestMatch<T> lastMatch) {
        if (lastMatch == null) {
            return null;
        }

        var initialMatches = requestPaths.match(path);
        var result = mapFromPathMatcher(path, initialMatches, 0);
        if (result != null) {
            int idx = nextMatchStartingIndex(initialMatches, lastMatch);
            return mapFromPathMatcher(path, initialMatches, idx);
        }

        // the following code is meant to handle cases like https://github.com/quarkusio/quarkus/issues/30667
        initialMatches = requestPaths.defaultMatch(path);
        result = mapFromPathMatcher(path, initialMatches, 0);
        if (result != null) {
            int idx = nextMatchStartingIndex(initialMatches, lastMatch);
            return mapFromPathMatcher(path, initialMatches, idx);
        }
        return null;
    }

    private int nextMatchStartingIndex(PathMatcher.PathMatch<ArrayList<RequestPath<T>>> initialMatches,
            RequestMatch<T> current) {
        if (initialMatches.getValue() == null || initialMatches.getValue().isEmpty()) {
            return -1;
        }
        for (int i = 0; i < initialMatches.getValue().size(); i++) {
            if (initialMatches.getValue().get(i).template == current.template) {
                i++;

                if (i < initialMatches.getValue().size()) {
                    return i;
                }
                return -1;
            }
        }

        return -1;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private RequestMatch<T> mapFromPathMatcher(String path, PathMatcher.PathMatch<ArrayList<RequestPath<T>>> initialMatches,
            int startIdx) {
        var value = initialMatches.getValue();
        if (value == null || startIdx < 0) {
            return null;
        }
        int pathLength = path.length();
        for (int index = startIdx; index < value.size(); index++) {
            RequestPath<T> potentialMatch = value.get(index);
            String[] params = (maxParams > 0) ? new String[maxParams] : EMPTY_STRING_ARRAY;
            int paramCount = 0;
            boolean matched = true;
            boolean prefixAllowed = potentialMatch.prefixTemplate;
            int matchPos = initialMatches.getMatched().length();
            for (int i = 1; i < potentialMatch.template.components.length; ++i) {
                URITemplate.TemplateComponent segment = potentialMatch.template.components[i];
                if (segment.type == URITemplate.Type.CUSTOM_REGEX) {
                    // exclude any path end slash when matching a subdir, but include it in the matched length
                    boolean endSlash = matchPos < path.length() && path.charAt(path.length() - 1) == '/';
                    Matcher matcher = segment.pattern.matcher(
                            endSlash ? path.substring(0, path.length() - 1) : path);
                    matched = matcher.find(matchPos);
                    if (!matched || matcher.start() != matchPos) {
                        break;
                    }
                    matchPos = matcher.end();
                    if (endSlash) {
                        matchPos++;
                    }
                    for (String group : segment.groups) {
                        params[paramCount++] = matcher.group(group);
                    }
                } else if (segment.type == URITemplate.Type.LITERAL) {
                    //make sure the literal text is the same
                    if (matchPos + segment.literalText.length() > pathLength) {
                        matched = false;
                        break; //too long
                    }
                    for (int pos = 0; pos < segment.literalText.length(); ++pos) {
                        if (path.charAt(matchPos++) != segment.literalText.charAt(pos)) {
                            matched = false;
                            break;
                        }
                    }
                    if (!matched) {
                        break;
                    }
                } else if (segment.type == URITemplate.Type.DEFAULT_REGEX) {
                    if (matchPos == pathLength) {
                        matched = false;
                        break;
                    }
                    int start = matchPos;
                    while (matchPos < pathLength && path.charAt(matchPos) != '/') {
                        matchPos++;
                    }
                    params[paramCount++] = path.substring(start, matchPos);
                }
            }
            if (!matched) {
                continue;
            }
            if (paramCount < params.length) {
                params[paramCount] = null;
            }
            boolean fullMatch = matchPos == pathLength;
            boolean doPrefixMatch = false;
            if (!fullMatch) {
                //according to the spec every template ends with (/.*)?
                if (matchPos == 1) { //matchPos == 1 corresponds to '/' as a root level match
                    doPrefixMatch = prefixAllowed || pathLength == 1; //if prefix is allowed, or we've matched the whole thing
                } else if (path.charAt(matchPos) == '/') {
                    doPrefixMatch = prefixAllowed || matchPos == pathLength - 1; //if prefix is allowed, or the remainder is only a trailing /
                }
            }
            if (fullMatch || doPrefixMatch) {
                String remaining;
                if (fullMatch) {
                    remaining = "";
                } else {
                    if (matchPos == 1) {
                        remaining = path;
                    } else {
                        remaining = path.substring(matchPos);
                    }
                }
                return new RequestMatch(potentialMatch.template, potentialMatch.value, params, remaining);
            }
        }
        return null;
    }

    public static class RequestPath<T> implements Dumpable, Comparable<RequestPath<T>> {
        public final boolean prefixTemplate;
        public final URITemplate template;
        public final T value;

        public RequestPath(boolean prefixTemplate, URITemplate template, T value) {
            this.prefixTemplate = prefixTemplate;
            this.template = template;
            this.value = value;
        }

        @Override
        public String toString() {
            return "RequestPath{ value: " + value + ", template: " + template + " }";
        }

        @Override
        public void dump(int level) {
            indent(level);
            System.err.println("RequestPath:");
            indent(level + 1);
            System.err.println("value: " + value);
            indent(level + 1);
            System.err.println("template: ");
            template.dump(level + 2);
        }

        @Override
        public int compareTo(RequestPath<T> o) {
            return o.template.compareTo(this.template);
        }
    }

    public static class RequestMatch<T> {
        public final URITemplate template;
        public final T value;
        /**
         * The matched parameters in order.
         * <p>
         * Note that this array may be larger than required, and padded with null values at the end
         */
        public final String[] pathParamValues;
        public final String remaining;

        public RequestMatch(URITemplate template, T value, String[] pathParamValues, String remaining) {
            this.template = template;
            this.value = value;
            this.pathParamValues = pathParamValues;
            this.remaining = remaining;
        }

        @Override
        public String toString() {
            return "RequestMatch{ value: " + value + ", template: " + template + ", pathParamValues: "
                    + Arrays.toString(pathParamValues) + " }";
        }
    }

    public void dump() {
        this.requestPaths.dump(0);
    }

    public PathMatcher<ArrayList<RequestPath<T>>> getRequestPaths() {
        return requestPaths;
    }

    public ArrayList<RequestPath<T>> getTemplates() {
        return templates;
    }
}
