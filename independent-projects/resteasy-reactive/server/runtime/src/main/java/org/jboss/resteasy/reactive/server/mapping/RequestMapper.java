package org.jboss.resteasy.reactive.server.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import org.jboss.resteasy.reactive.common.util.URIDecoder;

public class RequestMapper<T> {

    private final PathMatcher<List<RequestPath<T>>> requestPaths;
    private final List<RequestPath<T>> templates;
    final int maxParams;

    public RequestMapper(List<RequestPath<T>> templates) {
        this.requestPaths = new PathMatcher<>();
        this.templates = templates;
        int max = 0;
        Map<String, List<RequestPath<T>>> aggregates = new HashMap<>();
        for (RequestPath<T> i : templates) {
            List<RequestPath<T>> paths = aggregates.get(i.template.stem);
            if (paths == null) {
                aggregates.put(i.template.stem, paths = new ArrayList<>());
            }
            paths.add(i);
            max = Math.max(max, i.template.countPathParamNames());
        }
        aggregates.forEach(this::sortAggregates);
        aggregates.forEach(this::addPrefixPaths);
        maxParams = max;
    }

    private void sortAggregates(String stem, List<RequestPath<T>> list) {
        list.sort(new Comparator<RequestPath<T>>() {
            @Override
            public int compare(RequestPath<T> t1, RequestPath<T> t2) {
                return t2.template.compareTo(t1.template);
            }
        });
    }

    private void addPrefixPaths(String stem, List<RequestPath<T>> list) {
        requestPaths.addPrefixPath(stem, list);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public RequestMatch<T> map(String path) {
        int pathLength = path.length();
        PathMatcher.PathMatch<List<RequestPath<T>>> initialMatch = requestPaths.match(path);
        if (initialMatch.getValue() == null) {
            return null;
        }

        List<RequestPath<T>> value = initialMatch.getValue();
        for (int index = 0; index < value.size(); index++) {
            RequestPath<T> potentialMatch = value.get(index);
            String[] params = new String[maxParams];
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
                    for (String name : segment.names) {
                        params[paramCount++] = URIDecoder.decodeURIComponent(matcher.group(name), false);
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
                } else if (segment.type == URITemplate.Type.DEFAULT_REGEX) {
                    if (matchPos == pathLength) {
                        matched = false;
                        break;
                    }
                    int start = matchPos;
                    while (matchPos < pathLength && path.charAt(matchPos) != '/') {
                        matchPos++;
                    }
                    params[paramCount++] = URIDecoder.decodeURIComponent(path.substring(start, matchPos), false);
                }
            }
            if (paramCount < params.length) {
                params[paramCount] = null;
            }
            boolean fullMatch = matchPos == pathLength;
            if (!prefixAllowed && !fullMatch) {
                //according to the spec every template ends with (/.*)?
                prefixAllowed = path.charAt(matchPos) == '/' && matchPos == pathLength - 1;
            }
            if (matched && (fullMatch || prefixAllowed)) {
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

    public PathMatcher<List<RequestPath<T>>> getRequestPaths() {
        return requestPaths;
    }

    public List<RequestPath<T>> getTemplates() {
        return templates;
    }
}
