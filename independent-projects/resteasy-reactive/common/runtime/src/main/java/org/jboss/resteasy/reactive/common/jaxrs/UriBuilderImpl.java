package org.jboss.resteasy.reactive.common.jaxrs;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;
import jakarta.ws.rs.ext.RuntimeDelegate;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.resteasy.reactive.common.util.Encode;
import org.jboss.resteasy.reactive.common.util.PathHelper;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class UriBuilderImpl extends UriBuilder {

    private static final class URITemplateParametersMap extends HashMap<String, Object> {

        private final Object[] parameterValues;
        private int index;

        private URITemplateParametersMap(final Object... parameterValues) {
            this.parameterValues = parameterValues;
        }

        public Object get(Object key) {
            Object object = null;
            if (!super.containsKey(key) && this.index != this.parameterValues.length) {
                object = this.parameterValues[this.index++];
                super.put((String) key, object);
            } else {
                object = super.get(key);
            }
            return object;
        }

        public boolean containsKey(Object key) {
            boolean containsKey = super.containsKey(key);
            if (!containsKey && this.index != this.parameterValues.length) {
                super.put((String) key, this.parameterValues[this.index++]);
                containsKey = true;
            }
            return containsKey;
        }

    }

    private String host;
    private String scheme;
    private int port = -1;

    private String userInfo;
    private String path;
    private String query;
    private String fragment;
    private String ssp;
    private String authority;

    private MultiQueryParamMode queryParamMode = MultiQueryParamMode.MULTI_PAIRS;

    public UriBuilder clone() {
        UriBuilderImpl impl = new UriBuilderImpl();
        impl.host = host;
        impl.scheme = scheme;
        impl.port = port;
        impl.userInfo = userInfo;
        impl.path = path;
        impl.query = query;
        impl.fragment = fragment;
        impl.ssp = ssp;
        impl.authority = authority;
        impl.queryParamMode = queryParamMode;

        return impl;
    }

    public static final Pattern opaqueUri = Pattern.compile("^([^:/?#{]+):([^/].*)");
    public static final Pattern hierarchicalUri = Pattern
            .compile("^(([^:/?#{]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");

    public static boolean compare(String s1, String s2) {
        if (s1 == s2)
            return true;
        if (s1 == null || s2 == null)
            return false;
        return s1.equals(s2);
    }

    public static URI relativize(URI from, URI to) {
        if (!compare(from.getScheme(), to.getScheme()))
            return to;
        if (!compare(from.getHost(), to.getHost()))
            return to;
        if (from.getPort() != to.getPort())
            return to;
        if (from.getPath() == null && to.getPath() == null)
            return URI.create("");
        else if (from.getPath() == null)
            return URI.create(to.getPath());
        else if (to.getPath() == null)
            return to;

        String fromPath = from.getPath();
        if (fromPath.startsWith("/"))
            fromPath = fromPath.substring(1);
        String[] fsplit = fromPath.split("/");
        String toPath = to.getPath();
        if (toPath.startsWith("/"))
            toPath = toPath.substring(1);
        String[] tsplit = toPath.split("/");

        int f = 0;

        for (; f < fsplit.length && f < tsplit.length; f++) {
            if (!fsplit[f].equals(tsplit[f]))
                break;
        }

        UriBuilder builder = UriBuilder.fromPath("");
        for (int i = f; i < fsplit.length; i++)
            builder.path("..");
        for (int i = f; i < tsplit.length; i++)
            builder.path(tsplit[i]);
        return builder.build();
    }

    /**
     * You may put path parameters anywhere within the uriTemplate except port.
     *
     * @param uriTemplate uri template
     * @return uri builder
     */
    public static UriBuilderImpl fromTemplate(String uriTemplate) {
        UriBuilderImpl impl = (UriBuilderImpl) RuntimeDelegate.getInstance().createUriBuilder();
        impl.uriTemplate(uriTemplate);
        return impl;
    }

    private static final Pattern hostPortPattern = Pattern.compile("([^/:]+):(\\d+)");
    private static final Pattern squareHostBrackets = Pattern
            .compile("(\\[(([0-9A-Fa-f]{0,4}:){2,7})([0-9A-Fa-f]{0,4})%?.*\\]):(\\d+)");

    /**
     * You may put path parameters anywhere within the uriTemplate except port.
     *
     * @param uriTemplate uri template
     * @return uri builder
     */
    public UriBuilder uriTemplate(CharSequence uriTemplate) {
        if (uriTemplate == null)
            throw new IllegalArgumentException("Param was null");
        Matcher opaque = opaqueUri.matcher(uriTemplate);
        if (opaque.matches()) {
            this.authority = null;
            this.host = null;
            this.port = -1;
            this.userInfo = null;
            this.query = null;
            this.scheme = opaque.group(1);
            this.ssp = opaque.group(2);
            return this;
        } else {
            Matcher match = hierarchicalUri.matcher(uriTemplate);
            if (match.matches()) {
                ssp = null;
                return parseHierarchicalUri(uriTemplate, match);
            }
        }
        throw new IllegalArgumentException("Illegal URI template" + uriTemplate);
    }

    protected UriBuilder parseHierarchicalUri(CharSequence uriTemplate, Matcher match) {
        boolean scheme = match.group(2) != null;
        if (scheme)
            this.scheme = match.group(2);
        String authority = match.group(4);
        if (authority != null) {
            this.authority = null;
            String host = match.group(4);
            int at = host.indexOf('@');
            if (at > -1) {
                String user = host.substring(0, at);
                host = host.substring(at + 1);
                this.userInfo = user;
            }

            Matcher hostPortMatch = hostPortPattern.matcher(host);
            if (hostPortMatch.matches()) {
                this.host = hostPortMatch.group(1);
                try {
                    this.port = Integer.parseInt(hostPortMatch.group(2));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Illegal URI template" + uriTemplate, e);
                }
            } else {
                if (host.startsWith("[")) {
                    // Must support an IPv6 hostname of format "[::1]" or [0:0:0:0:0:0:0:0]
                    // and IPv6 link-local format [fe80::1234%1] [ff08::9abc%interface10]
                    Matcher bracketsMatch = squareHostBrackets.matcher(host);
                    if (bracketsMatch.matches()) {
                        host = bracketsMatch.group(1);
                        try {
                            this.port = Integer.parseInt(bracketsMatch.group(5));
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Illegal URI template" + uriTemplate, e);
                        }
                    }
                }
                this.host = host;
            }
        }
        if (match.group(5) != null) {
            String group = match.group(5);
            if (!scheme && !"".equals(group) && !group.startsWith("/") && group.indexOf(':') > -1 &&
                    group.indexOf('/') > -1 && group.indexOf(':') < group.indexOf('/'))
                throw new IllegalArgumentException("Illegal URI template" + uriTemplate);
            if (!"".equals(group))
                replacePath(group);
        }
        if (match.group(7) != null)
            replaceQuery(match.group(7));
        if (match.group(9) != null)
            fragment(match.group(9));
        return this;
    }

    public UriBuilder uri(String uriTemplate) throws IllegalArgumentException {
        return uriTemplate(uriTemplate);
    }

    public UriBuilder uriFromCharSequence(CharSequence uriTemplate) throws IllegalArgumentException {
        return uriTemplate(uriTemplate);
    }

    public UriBuilder uri(URI uri) throws IllegalArgumentException {
        if (uri == null)
            throw new IllegalArgumentException("URI is null");

        if (uri.getRawFragment() != null)
            fragment = uri.getRawFragment();

        if (uri.isOpaque()) {
            scheme = uri.getScheme();
            ssp = uri.getRawSchemeSpecificPart();
            return this;
        }

        if (uri.getScheme() == null) {
            if (ssp != null) {
                if (uri.getRawSchemeSpecificPart() != null) {
                    ssp = uri.getRawSchemeSpecificPart();
                    return this;
                }
            }
        } else {
            scheme = uri.getScheme();
        }

        ssp = null;
        if (uri.getRawAuthority() != null) {
            if (uri.getRawUserInfo() == null && uri.getHost() == null && uri.getPort() == -1) {
                authority = uri.getRawAuthority();
                userInfo = null;
                host = null;
                port = -1;
            } else {
                authority = null;
                if (uri.getRawUserInfo() != null) {
                    userInfo = uri.getRawUserInfo();
                }
                if (uri.getHost() != null) {
                    host = uri.getHost();
                }
                if (uri.getPort() != -1) {
                    port = uri.getPort();
                }
            }
        }

        if (uri.getRawPath() != null && uri.getRawPath().length() > 0) {
            path = uri.getRawPath();
        }
        if (uri.getRawQuery() != null && uri.getRawQuery().length() > 0) {
            query = uri.getRawQuery();
        }

        return this;
    }

    public UriBuilder scheme(String scheme) throws IllegalArgumentException {
        this.scheme = scheme;
        return this;
    }

    public UriBuilder schemeSpecificPart(String ssp) throws IllegalArgumentException {
        if (ssp == null)
            throw new IllegalArgumentException("Scheme was null");

        StringBuilder sb = new StringBuilder();
        if (scheme != null)
            sb.append(scheme).append(':');
        if (ssp != null)
            sb.append(ssp);
        if (fragment != null && fragment.length() > 0)
            sb.append('#').append(fragment);
        URI uri = URI.create(sb.toString());

        if (uri.getRawSchemeSpecificPart() != null && uri.getRawPath() == null) {
            this.ssp = uri.getRawSchemeSpecificPart();
        } else {
            this.ssp = null;
            userInfo = uri.getRawUserInfo();
            host = uri.getHost();
            port = uri.getPort();
            path = uri.getRawPath();
            query = uri.getRawQuery();

        }
        return this;

    }

    public UriBuilder userInfo(String ui) {
        this.userInfo = ui;
        return this;
    }

    public UriBuilder host(String host) throws IllegalArgumentException {
        if (host != null && host.equals(""))
            throw new IllegalArgumentException("invalid host");
        this.host = host;
        return this;
    }

    public UriBuilder port(int port) throws IllegalArgumentException {
        if (port < -1)
            throw new IllegalArgumentException("invalid port");
        this.port = port;
        return this;
    }

    protected static String paths(boolean encode, String basePath, String... segments) {
        String path = basePath;
        if (path == null)
            path = "";
        for (String segment : segments) {
            if ("".equals(segment))
                continue;
            if (path.endsWith("/")) {
                if (segment.startsWith("/")) {
                    segment = segment.substring(1);
                    if ("".equals(segment))
                        continue;
                }
                if (encode)
                    segment = Encode.encodePath(segment);
                path += segment;
            } else {
                if (encode)
                    segment = Encode.encodePath(segment);
                if ("".equals(path)) {
                    path = segment;
                } else if (segment.startsWith("/")) {
                    path += segment;
                } else {
                    path += "/" + segment;
                }
            }

        }
        return path;
    }

    public UriBuilder path(String segment) throws IllegalArgumentException {
        if (segment == null)
            throw new IllegalArgumentException("path is null");
        path = paths(true, path, segment);
        return this;
    }

    @SuppressWarnings("unchecked")
    public UriBuilder path(Class resource) throws IllegalArgumentException {
        if (resource == null)
            throw new IllegalArgumentException("path is null");
        Path ann = (Path) resource.getAnnotation(Path.class);
        if (ann != null) {
            String[] segments = new String[] { ann.value() };
            path = paths(true, path, segments);
        } else {
            throw new IllegalArgumentException("class must be annotated with @Path");
        }
        return this;
    }

    public UriBuilder path(Class resource, String method) throws IllegalArgumentException {
        if (resource == null)
            throw new IllegalArgumentException("resource is null");
        if (method == null)
            throw new IllegalArgumentException("method is null");
        Method theMethod = null;
        for (Method m : resource.getMethods()) {
            if (m.getName().equals(method)) {
                if (theMethod != null && m.isAnnotationPresent(Path.class)) {
                    throw new IllegalArgumentException("Two methods with the same path " + method);
                }
                if (m.isAnnotationPresent(Path.class))
                    theMethod = m;
            }
        }
        if (theMethod == null)
            throw new IllegalArgumentException("No public method annotated with @Path " + resource.getName() + " " + method);
        return path(theMethod);
    }

    public UriBuilder path(Method method) throws IllegalArgumentException {
        if (method == null) {
            throw new IllegalArgumentException("method is null");
        }
        Path ann = method.getAnnotation(Path.class);
        if (ann != null) {
            path = paths(true, path, ann.value());
        } else {
            throw new IllegalArgumentException("Method not annotated with @Path");
        }
        return this;
    }

    public UriBuilder replaceMatrix(String matrix) throws IllegalArgumentException {
        if (matrix == null)
            matrix = "";
        if (!matrix.startsWith(";"))
            matrix = ";" + matrix;
        matrix = Encode.encodePath(matrix);
        if (path == null) {
            path = matrix;
        } else {
            int start = path.lastIndexOf('/');
            if (start < 0)
                start = 0;
            int matrixIndex = path.indexOf(';', start);
            if (matrixIndex > -1)
                path = path.substring(0, matrixIndex) + matrix;
            else
                path += matrix;

        }
        return this;
    }

    public UriBuilder replaceQuery(String query) throws IllegalArgumentException {
        if (query == null || query.length() == 0) {
            this.query = null;
            return this;
        }
        this.query = Encode.encodeQueryString(query);
        return this;
    }

    public UriBuilder fragment(String fragment) throws IllegalArgumentException {
        if (fragment == null) {
            this.fragment = null;
            return this;
        }
        this.fragment = Encode.encodeFragment(fragment);
        return this;
    }

    /**
     * Only replace path params in path of URI. This changes state of URIBuilder.
     *
     * @param name parameter name
     * @param value parameter value
     * @param isEncoded encoded flag
     * @return uri builder
     */
    public UriBuilder substitutePathParam(String name, Object value, boolean isEncoded) {
        if (path != null) {
            StringBuilder builder = new StringBuilder();
            replacePathParameter(name, value.toString(), isEncoded, path, builder, false);
            path = builder.toString();
        }
        return this;
    }

    public URI buildFromMap(Map<String, ? extends Object> values) throws IllegalArgumentException, UriBuilderException {
        if (values == null)
            throw new IllegalArgumentException("Values parameter is null");
        return buildUriFromMap(values, false, true);
    }

    public URI buildFromEncodedMap(Map<String, ? extends Object> values) throws IllegalArgumentException, UriBuilderException {
        if (values == null)
            throw new IllegalArgumentException("Values parameter is null");
        return buildUriFromMap(values, true, false);
    }

    public URI buildFromMap(Map<String, ?> values, boolean encodeSlashInPath)
            throws IllegalArgumentException, UriBuilderException {
        if (values == null)
            throw new IllegalArgumentException("Values parameter is null");
        return buildUriFromMap(values, false, encodeSlashInPath);
    }

    protected URI buildUriFromMap(Map<String, ? extends Object> paramMap, boolean fromEncodedMap, boolean encodeSlash)
            throws IllegalArgumentException, UriBuilderException {
        String buf = buildString(paramMap, fromEncodedMap, false, encodeSlash);
        try {
            return URI.create(buf);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create URI", e);
        }
    }

    private String buildString(Map<String, ? extends Object> paramMap, boolean fromEncodedMap, boolean isTemplate,
            boolean encodeSlash) {
        return buildCharSequence(paramMap, fromEncodedMap, isTemplate, encodeSlash).toString();
    }

    private CharSequence buildCharSequence(Map<String, ? extends Object> paramMap, boolean fromEncodedMap, boolean isTemplate,
            boolean encodeSlash) {
        StringBuilder builder = new StringBuilder();

        if (scheme != null)
            replaceParameter(paramMap, fromEncodedMap, isTemplate, scheme, builder, encodeSlash).append(":");
        if (ssp != null) {
            builder.append(ssp);
        } else if (userInfo != null || host != null || port != -1) {
            builder.append("//");
            if (userInfo != null)
                replaceParameter(paramMap, fromEncodedMap, isTemplate, userInfo, builder, encodeSlash).append("@");
            if (host != null) {
                if ("".equals(host))
                    throw new UriBuilderException("empty host");
                replaceParameter(paramMap, fromEncodedMap, isTemplate, host, builder, encodeSlash);
            }
            if (port != -1)
                builder.append(":").append(Integer.toString(port));
        } else if (authority != null) {
            builder.append("//");
            replaceParameter(paramMap, fromEncodedMap, isTemplate, authority, builder, encodeSlash);
        }
        if (path != null) {
            StringBuilder tmp = new StringBuilder();
            replaceParameter(paramMap, fromEncodedMap, isTemplate, path, tmp, encodeSlash);
            if (userInfo != null || host != null) {
                if (tmp.length() > 0 && tmp.charAt(0) != '/')
                    builder.append("/");
            }
            builder.append(tmp);
        }
        if (query != null) {
            builder.append("?");
            replaceQueryStringParameter(paramMap, fromEncodedMap, isTemplate, query, builder);
        }
        if (fragment != null) {
            builder.append("#");
            replaceParameter(paramMap, fromEncodedMap, isTemplate, fragment, builder, encodeSlash);
        }
        return builder;
    }

    protected StringBuilder replacePathParameter(String name, String value, boolean isEncoded, String string,
            StringBuilder builder, boolean encodeSlash) {
        if (string.indexOf('{') == -1) {
            return builder.append(string);
        }
        Matcher matcher = createUriParamMatcher(string);
        int start = 0;
        while (matcher.find()) {
            String param = matcher.group(1);
            if (!param.equals(name))
                continue;
            builder.append(string, start, matcher.start());
            if (!isEncoded) {
                if (encodeSlash)
                    value = Encode.encodePath(value);
                else
                    value = Encode.encodePathSegment(value);

            } else {
                value = Encode.encodeNonCodes(value);
            }
            builder.append(value);
            start = matcher.end();
        }
        builder.append(string, start, string.length());
        return builder;
    }

    public static Matcher createUriParamMatcher(String string) {
        Matcher matcher = PathHelper.URI_PARAM_PATTERN.matcher(PathHelper.replaceEnclosedCurlyBracesCS(string));
        return matcher;
    }

    protected StringBuilder replaceParameter(Map<String, ? extends Object> paramMap, boolean fromEncodedMap, boolean isTemplate,
            String string, StringBuilder builder, boolean encodeSlash) {
        if (string.indexOf('{') == -1) {
            return builder.append(string);
        }
        Matcher matcher = createUriParamMatcher(string);
        int start = 0;
        while (matcher.find()) {
            builder.append(string, start, matcher.start());
            String param = matcher.group(1);
            boolean containsValueForParam = paramMap.containsKey(param);
            if (!containsValueForParam) {
                if (isTemplate) {
                    builder.append(matcher.group());
                    start = matcher.end();
                    continue;
                }
                throw new IllegalArgumentException("Path parameter not provided " + param);
            }
            Object value = paramMap.get(param);
            String stringValue = value != null ? value.toString() : null;
            if (stringValue != null) {
                if (!fromEncodedMap) {
                    if (encodeSlash)
                        stringValue = Encode.encodePathSegmentAsIs(stringValue);
                    else
                        stringValue = Encode.encodePathAsIs(stringValue);
                } else {
                    if (encodeSlash)
                        stringValue = Encode.encodePathSegmentSaveEncodings(stringValue);
                    else
                        stringValue = Encode.encodePathSaveEncodings(stringValue);
                }
                builder.append(stringValue);
                start = matcher.end();
            } else {
                throw new IllegalArgumentException("Template parameter null: " + param);
            }
        }
        builder.append(string, start, string.length());
        return builder;
    }

    protected StringBuilder replaceQueryStringParameter(Map<String, ? extends Object> paramMap, boolean fromEncodedMap,
            boolean isTemplate, String string, StringBuilder builder) {
        if (string.indexOf('{') == -1) {
            return builder.append(string);
        }
        Matcher matcher = createUriParamMatcher(string);
        int start = 0;
        while (matcher.find()) {
            builder.append(string, start, matcher.start());
            String param = matcher.group(1);
            boolean containsValueForParam = paramMap.containsKey(param);
            if (!containsValueForParam) {
                if (isTemplate) {
                    builder.append(matcher.group());
                    start = matcher.end();
                    continue;
                }
                throw new IllegalArgumentException("Path parameter not provided " + param);
            }
            Object value = paramMap.get(param);
            String stringValue = value != null ? value.toString() : null;
            if (stringValue != null) {
                if (!fromEncodedMap) {
                    stringValue = Encode.encodeQueryParamAsIs(stringValue);
                } else {
                    stringValue = Encode.encodeQueryParamSaveEncodings(stringValue);
                }
                builder.append(stringValue);
                start = matcher.end();
            } else {
                throw new IllegalArgumentException("Template parm was null: " + param);
            }
        }
        builder.append(string, start, string.length());
        return builder;
    }

    /**
     * Return a unique order list of path params.
     *
     * @return list of path parameters
     */
    public List<String> getPathParamNamesInDeclarationOrder() {
        List<String> params = new ArrayList<String>();
        HashSet<String> set = new HashSet<String>();
        if (scheme != null)
            addToPathParamList(params, set, scheme);
        if (userInfo != null)
            addToPathParamList(params, set, userInfo);
        if (host != null)
            addToPathParamList(params, set, host);
        if (path != null)
            addToPathParamList(params, set, path);
        if (query != null)
            addToPathParamList(params, set, query);
        if (fragment != null)
            addToPathParamList(params, set, fragment);

        return params;
    }

    private void addToPathParamList(List<String> params, HashSet<String> set, String string) {
        Matcher matcher = PathHelper.URI_PARAM_PATTERN.matcher(PathHelper.replaceEnclosedCurlyBracesCS(string));
        while (matcher.find()) {
            String param = matcher.group(1);
            if (set.contains(param))
                continue;
            else {
                set.add(param);
                params.add(param);
            }
        }
    }

    public URI build(Object... values) throws IllegalArgumentException, UriBuilderException {
        if (values == null)
            throw new IllegalArgumentException("Values parameter is null");
        return buildFromValues(true, false, values);
    }

    protected URI buildFromValues(boolean encodeSlash, boolean encoded, Object... values) {
        String buf = null;
        try {
            buf = buildString(new URITemplateParametersMap(values), encoded, false, encodeSlash);
            return new URI(buf);
            //return URI.create(buf);
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception e) {
            throw new UriBuilderException("failed to create URI", e);
        }
    }

    public UriBuilder matrixParam(String name, Object... values) throws IllegalArgumentException {
        if (name == null)
            throw new IllegalArgumentException("Name parameter is null");
        if (values == null)
            throw new IllegalArgumentException("Values parameter is null");
        if (path == null)
            path = "";
        for (Object val : values) {
            if (val == null)
                throw new IllegalArgumentException("Value is null");
            path += ";" + Encode.encodeMatrixParam(name) + "=" + Encode.encodeMatrixParam(val.toString());
        }
        return this;
    }

    private static final Pattern PARAM_REPLACEMENT = Pattern.compile("_resteasy_uri_parameter");

    public UriBuilder replaceMatrixParam(String name, Object... values) throws IllegalArgumentException {
        if (name == null)
            throw new IllegalArgumentException("Name parameter is null");
        if (path == null) {
            if (values != null && values.length > 0)
                return matrixParam(name, values);
            return this;
        }

        // remove all path param expressions so we don't accidentally start replacing within a regular expression
        ArrayList<String> pathParams = new ArrayList<String>();
        boolean foundParam = false;

        CharSequence pathWithoutEnclosedCurlyBraces = PathHelper.replaceEnclosedCurlyBracesCS(this.path);
        Matcher matcher = PathHelper.URI_TEMPLATE_PATTERN.matcher(pathWithoutEnclosedCurlyBraces);
        StringBuilder newSegment = new StringBuilder();
        int from = 0;
        while (matcher.find()) {
            newSegment.append(pathWithoutEnclosedCurlyBraces, from, matcher.start());
            foundParam = true;
            String group = matcher.group();
            pathParams.add(PathHelper.recoverEnclosedCurlyBraces(group));
            newSegment.append("_resteasy_uri_parameter");
            from = matcher.end();
        }
        newSegment.append(pathWithoutEnclosedCurlyBraces, from, pathWithoutEnclosedCurlyBraces.length());
        path = newSegment.toString();

        // Find last path segment
        int start = path.lastIndexOf('/');
        if (start < 0)
            start = 0;

        int matrixIndex = path.indexOf(';', start);
        if (matrixIndex > -1) {

            String matrixParams = path.substring(matrixIndex + 1);
            path = path.substring(0, matrixIndex);
            MultivaluedMap<String, String> map = new QuarkusMultivaluedHashMap<String, String>();

            String[] params = matrixParams.split(";");
            for (String param : params) {
                int idx = param.indexOf('=');
                if (idx < 0) {
                    map.add(param, null);
                } else {
                    String theName = param.substring(0, idx);
                    String value = "";
                    if (idx + 1 < param.length())
                        value = param.substring(idx + 1);
                    map.add(theName, value);
                }
            }
            map.remove(name);
            for (String theName : map.keySet()) {
                List<String> vals = map.get(theName);
                for (Object val : vals) {
                    if (val == null)
                        path += ";" + theName;
                    else
                        path += ";" + theName + "=" + val.toString();
                }
            }
        }
        if (values != null && values.length > 0)
            matrixParam(name, values);

        // put back all path param expressions
        if (foundParam) {
            matcher = PARAM_REPLACEMENT.matcher(path);
            newSegment = new StringBuilder();
            int i = 0;
            from = 0;
            while (matcher.find()) {
                newSegment.append(this.path, from, matcher.start());
                newSegment.append(pathParams.get(i++));
                from = matcher.end();
            }
            newSegment.append(this.path, from, this.path.length());
            path = newSegment.toString();
        }
        return this;
    }

    /**
     * Called by ClientRequest.getUri() to add a query parameter for
     * {@code @QueryParam} parameters. We do not use UriBuilder.queryParam()
     * because
     * <ul>
     * <li>queryParam() supports URI template processing and this method must
     * always encode braces (for parameter substitution is not possible for
     * {@code @QueryParam} parameters).
     * <li>queryParam() supports "contextual URI encoding" (i.e., it does not
     * encode {@code %} characters that are followed by two hex characters).
     * The JavaDoc for {@code @QueryParam.value()} explicitly states that
     * the value is specified in decoded format and that "any percent
     * encoded literals within the value will not be decoded and will
     * instead be treated as literal text". This means that it is an
     * explicit bug to perform contextual URI encoding of this method's
     * name parameter; hence, we must always encode said parameter. This
     * method also foregoes contextual URI encoding on this method's values
     * parameter because it represents arbitrary data passed to a
     * {@code QueryParam} parameter of a client proxy (since the client
     * proxy is nothing more than a transport layer, it should not be
     * "interpreting" such data; instead, it should faithfully transmit
     * this data over the wire).
     * </ul>
     *
     * @param name the name of the query parameter.
     * @param values the value(s) of the query parameter.
     * @return Returns this instance to allow call chaining.
     */
    public UriBuilder clientQueryParam(String name, Object... values) throws IllegalArgumentException {
        StringBuilder sb = new StringBuilder();
        String prefix = "";
        if (query == null)
            query = "";
        else {
            sb.append(query).append("&");
        }

        if (name == null)
            throw new IllegalArgumentException("Name parameter is null");
        if (values == null)
            throw new IllegalArgumentException("Values parameter is null");

        if (queryParamMode == MultiQueryParamMode.COMMA_SEPARATED) {
            sb.append(Encode.encodeQueryParamAsIs(name)).append("=");
        }
        for (Object value : values) {
            if (value == null)
                throw new IllegalArgumentException("Value is null");

            sb.append(prefix);
            switch (queryParamMode) {
                case MULTI_PAIRS:
                    prefix = "&";
                    sb.append(Encode.encodeQueryParamAsIs(name)).append("=")
                            .append(Encode.encodeQueryParamAsIs(value.toString()));
                    break;
                case COMMA_SEPARATED:
                    prefix = ",";
                    sb.append(Encode.encodeQueryParamAsIs(value.toString()));
                    break;
                case ARRAY_PAIRS:
                    prefix = "&";
                    String queryParamConnector = arrayPairsConnector(values);
                    sb.append(Encode.encodeQueryParamAsIs(name)).append(queryParamConnector)
                            .append(Encode.encodeQueryParamAsIs(value.toString()));
                    break;
            }
        }

        query = sb.toString();
        return this;
    }

    public UriBuilder queryParam(String name, Object... values) throws IllegalArgumentException {
        StringBuilder sb = new StringBuilder();
        String prefix = "";
        if (query == null)
            query = "";
        else {
            sb.append(query).append("&");
        }

        if (name == null)
            throw new IllegalArgumentException("Name parameter is null");
        if (values == null)
            throw new IllegalArgumentException("Values parameter is null");

        if (queryParamMode == MultiQueryParamMode.COMMA_SEPARATED) {
            sb.append(Encode.encodeQueryParam(name)).append("=");
        }
        for (Object value : values) {
            if (value == null)
                throw new IllegalArgumentException("Value is null");

            sb.append(prefix);
            switch (queryParamMode) {
                case MULTI_PAIRS:
                    prefix = "&";
                    sb.append(Encode.encodeQueryParam(name)).append("=").append(Encode.encodeQueryParam(value.toString()));
                    break;
                case COMMA_SEPARATED:
                    prefix = ",";
                    sb.append(Encode.encodeQueryParam(value.toString()));
                    break;
                case ARRAY_PAIRS:
                    prefix = "&";
                    String queryParamConnector = arrayPairsConnector(values);
                    sb.append(Encode.encodeQueryParam(name)).append(queryParamConnector)
                            .append(Encode.encodeQueryParam(value.toString()));
                    break;
            }
        }

        query = sb.toString();
        return this;
    }

    private String arrayPairsConnector(Object[] values) {
        return values.length == 1 ? "=" : "[]=";
    }

    public UriBuilder replaceQueryParam(String name, Object... values) throws IllegalArgumentException {
        if (name == null)
            throw new IllegalArgumentException("Name parameter is null");
        if (query == null || query.equals("")) {
            if (values != null)
                return queryParam(name, values);
            return this;
        }

        String[] params = query.split("&");
        query = null;

        String replacedName = Encode.encodeQueryParam(name);

        for (String param : params) {
            int pos = param.indexOf('=');
            if (pos >= 0) {
                String paramName = param.substring(0, pos);
                if (paramName.equals(replacedName))
                    continue;
            } else {
                if (param.equals(replacedName))
                    continue;
            }
            if (query == null)
                query = "";
            else
                query += "&";
            query += param;
        }
        // don't set values if values is null
        if (values == null || values.length == 0)
            return this;
        return queryParam(name, values);
    }

    public String getHost() {
        return host;
    }

    public String getScheme() {
        return scheme;
    }

    public int getPort() {
        return port;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public String getPath() {
        return path;
    }

    public String getQuery() {
        return query;
    }

    public String getFragment() {
        return fragment;
    }

    public UriBuilder segment(String... segments) throws IllegalArgumentException {
        if (segments == null)
            throw new IllegalArgumentException("Segment parameter is null");
        for (String segment : segments) {
            if (segment == null)
                throw new IllegalArgumentException("Segment is null");
            path(Encode.encodePathSegment(segment));
        }
        return this;
    }

    public URI buildFromEncoded(Object... values) throws IllegalArgumentException, UriBuilderException {
        if (values == null)
            throw new IllegalArgumentException("Values is null");
        return buildFromValues(false, true, values);
    }

    public UriBuilder replacePath(String path) {
        if (path == null) {
            this.path = null;
            return this;
        }
        this.path = Encode.encodePath(path);
        return this;
    }

    public URI build(Object[] values, boolean encodeSlashInPath) throws IllegalArgumentException, UriBuilderException {
        if (values == null)
            throw new IllegalArgumentException("Values is null");
        return buildFromValues(encodeSlashInPath, false, values);
    }

    public String toTemplate() {
        return buildString(new HashMap<String, Object>(), true, true, true);
    }

    public UriBuilder resolveTemplate(String name, Object value) throws IllegalArgumentException {
        if (name == null)
            throw new IllegalArgumentException("Name is null");
        if (value == null)
            throw new IllegalArgumentException("Value is null");
        HashMap<String, Object> vals = new HashMap<String, Object>();
        vals.put(name, value);
        return resolveTemplates(vals);
    }

    public UriBuilder resolveTemplates(Map<String, Object> templateValues) throws IllegalArgumentException {
        if (templateValues == null)
            throw new IllegalArgumentException("Template values null");
        if (templateValues.containsKey(null))
            throw new IllegalArgumentException("map key null");
        return uriTemplate(buildCharSequence(templateValues, false, true, true));
    }

    public UriBuilder resolveTemplate(String name, Object value, boolean encodeSlashInPath) throws IllegalArgumentException {
        if (name == null)
            throw new IllegalArgumentException("Name is null");
        if (value == null)
            throw new IllegalArgumentException("Value is null");
        HashMap<String, Object> vals = new HashMap<String, Object>();
        vals.put(name, value);
        return uriTemplate(buildCharSequence(vals, false, true, encodeSlashInPath));
    }

    public UriBuilder resolveTemplateFromEncoded(String name, Object value) throws IllegalArgumentException {
        if (name == null)
            throw new IllegalArgumentException("Name is null");
        if (value == null)
            throw new IllegalArgumentException("Value is null");
        HashMap<String, Object> vals = new HashMap<String, Object>();
        vals.put(name, value);
        return uriTemplate(buildCharSequence(vals, true, true, true));
    }

    public UriBuilder resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath)
            throws IllegalArgumentException {
        if (templateValues == null)
            throw new IllegalArgumentException("Template values null");
        if (templateValues.containsKey(null))
            throw new IllegalArgumentException("map key null");
        return uriTemplate(buildCharSequence(templateValues, false, true, encodeSlashInPath));
    }

    public UriBuilder resolveTemplatesFromEncoded(Map<String, Object> templateValues) throws IllegalArgumentException {
        if (templateValues == null)
            throw new IllegalArgumentException("Template values null");
        if (templateValues.containsKey(null))
            throw new IllegalArgumentException("map key null");
        return uriTemplate(buildCharSequence(templateValues, true, true, true));
    }

    public UriBuilder multiQueryParamMode(MultiQueryParamMode mode) {
        queryParamMode = mode;
        return this;
    }
}
