package org.jboss.resteasy.reactive.server.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        aggregates.forEach(this::sortAggregates);
        aggregates.forEach(this::addPrefixPaths);
        maxParams = max;
        requestPaths = pathMatcherBuilder.build();
    }

    private void sortAggregates(String stem, List<RequestPath<T>> list) {
        list.sort(new Comparator<RequestPath<T>>() {
            @Override
            public int compare(RequestPath<T> t1, RequestPath<T> t2) {
                return t2.template.compareTo(t1.template);
            }
        });
    }

    private void addPrefixPaths(String stem, ArrayList<RequestPath<T>> list) {
        pathMatcherBuilder.addPrefixPath(stem, list);
    }

    public RequestMatch<T> map(String path) {
        var result = mapFromPathMatcher(path, requestPaths.match(path));
        if (result != null) {
            return result;
        }

        // the following code is meant to handle cases like https://github.com/quarkusio/quarkus/issues/30667
        return mapFromPathMatcher(path, requestPaths.defaultMatch(path));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private RequestMatch<T> mapFromPathMatcher(String path, PathMatcher.PathMatch<ArrayList<RequestPath<T>>> initialMatch) {
        var value = initialMatch.getValue();
        if (initialMatch.getValue() == null) {
            return null;
        }
        int pathLength = path.length();
        for (int index = 0; index < ((List<RequestPath<T>>) value).size(); index++) {
            RequestPath<T> potentialMatch = ((List<RequestPath<T>>) value).get(index);
            String[] params = (maxParams > 0) ? new String[maxParams] : EMPTY_STRING_ARRAY;
            int paramCount = 0;
            boolean matched = true;
            boolean prefixAllowed = potentialMatch.prefixTemplate;
            int matchPos = initialMatch.getMatched().length();
            for (int i = 1; i < potentialMatch.template.components.length; ++i) {
                URITemplate.TemplateComponent segment = potentialMatch.template.components[i];
                if (segment.type == URITemplate.Type.CUSTOM_REGEX) {
                    Matcher matcher = segment.pattern.matcher(path);
                    matched = matcher.find(matchPos);
                    if (!matched || matcher.start() != matchPos) {
                        break;
                    }
                    matchPos = matcher.end();
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

    public static class RequestPath<T> implements Dumpable {
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
