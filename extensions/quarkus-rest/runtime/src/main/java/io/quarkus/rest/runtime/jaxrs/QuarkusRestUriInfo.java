package io.quarkus.rest.runtime.jaxrs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import io.quarkus.rest.runtime.core.QuarkusRestDeployment;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.core.UriMatch;
import io.quarkus.rest.runtime.util.PathSegmentImpl;
import io.quarkus.rest.runtime.util.QuarkusMultivaluedHashMap;
import io.quarkus.rest.runtime.util.UnmodifiableMultivaluedMap;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.impl.URIDecoder;

/**
 * UriInfo implementation
 */
public class QuarkusRestUriInfo implements UriInfo {

    private final QuarkusRestRequestContext currentRequest;
    private MultivaluedMap<String, String> queryParams;
    private MultivaluedMap<String, String> pathParams;
    private URI requestUri;

    public QuarkusRestUriInfo(QuarkusRestRequestContext currentRequest) {
        this.currentRequest = currentRequest;
    }

    @Override
    public String getPath() {
        return getPath(true);
    }

    @Override
    public String getPath(boolean decode) {
        if (!decode)
            throw encodedNotSupported();
        // TCK says normalized
        String path = URIDecoder.decodeURIComponent(currentRequest.getContext().normalisedPath(), false);
        // the path must not contain the prefix
        String prefix = currentRequest.getDeployment().getPrefix();
        if (prefix.isEmpty())
            return path;
        // else skip the prefix
        return path.substring(prefix.length());
    }

    @Override
    public List<PathSegment> getPathSegments() {
        return getPathSegments(true);
    }

    @Override
    public List<PathSegment> getPathSegments(boolean decode) {
        if (!decode)
            throw encodedNotSupported();
        return PathSegmentImpl.parseSegments(getPath(), decode);
    }

    @Override
    public URI getRequestUri() {
        if (requestUri == null) {
            HttpServerRequest request = currentRequest.getContext().request();
            try {
                // TCK says normalized
                requestUri = new URI(request.absoluteURI() + (request.query() == null ? "" : "?" + request.query()))
                        .normalize();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return requestUri;
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
        return UriBuilder.fromUri(getRequestUri());
    }

    @Override
    public URI getAbsolutePath() {
        HttpServerRequest request = currentRequest.getContext().request();
        try {
            // TCK says normalized
            return new URI(request.absoluteURI()).normalize();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        return UriBuilder.fromUri(getAbsolutePath());
    }

    @Override
    public URI getBaseUri() {
        HttpServerRequest req = currentRequest.getContext().request();
        try {
            String host = req.host();
            int port = -1;
            int index = host.indexOf(":");
            if (index > -1) {
                port = Integer.parseInt(host.substring(index + 1));
                host = host.substring(0, index);
            }
            QuarkusRestDeployment deployment = currentRequest.getDeployment();
            // the TCK doesn't tell us, but Stuart and Georgios prefer dressing their base URIs with useless slashes ;)
            String prefix = "/";
            if (deployment != null) {
                // prefix can be empty, but if it's not it will not end with a slash
                prefix = deployment.getPrefix();
                if (prefix.isEmpty())
                    prefix = "/";
                else
                    prefix = prefix + "/";
            }
            return new URI(req.scheme(), null, host, port,
                    prefix,
                    null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        return UriBuilder.fromUri(getBaseUri());
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        return getPathParameters(true);
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        if (!decode)
            throw encodedNotSupported();
        if (pathParams == null) {
            pathParams = new QuarkusMultivaluedHashMap<>();
            for (Entry<String, Integer> pathParam : currentRequest.getTarget().getPathParameterIndexes().entrySet()) {
                pathParams.add(pathParam.getKey(), currentRequest.getPathParam(pathParam.getValue()));
            }
        }
        return new UnmodifiableMultivaluedMap<>(pathParams);
    }

    private RuntimeException encodedNotSupported() {
        return new IllegalArgumentException("We do not support non-decoded parameters");
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        return getQueryParameters(true);
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        if (!decode)
            throw encodedNotSupported();
        if (queryParams == null) {
            queryParams = new QuarkusMultivaluedHashMap<>();
            MultiMap entries = currentRequest.getContext().queryParams();
            for (String i : entries.names()) {
                queryParams.addAll(i, entries.getAll(i));
            }
        }
        return new UnmodifiableMultivaluedMap<>(queryParams);
    }

    @Override
    public List<String> getMatchedURIs() {
        return getMatchedURIs(true);
    }

    @Override
    public List<String> getMatchedURIs(boolean decode) {
        if (!decode)
            throw encodedNotSupported();
        if (currentRequest.getTarget() == null) {
            return Collections.emptyList();
        }
        List<UriMatch> oldMatches = currentRequest.getMatchedURIs();
        List<String> matched = new ArrayList<>();
        String last = null;

        for (int i = 0; i < oldMatches.size(); ++i) {
            String m = oldMatches.get(i).matched;
            if (!m.equals(last)) {
                matched.add(m);
                last = m;
            }
        }
        return matched;
    }

    @Override
    public List<Object> getMatchedResources() {
        List<UriMatch> oldMatches = currentRequest.getMatchedURIs();
        List<Object> matched = new ArrayList<>();
        for (int i = 0; i < oldMatches.size(); ++i) {
            Object target = oldMatches.get(i).target;
            if (target != null) {
                matched.add(target);
            }
        }
        return matched;
    }

    @Override
    public URI resolve(URI uri) {
        return null;
    }

    @Override
    public URI relativize(URI uri) {
        return null;
    }
}
