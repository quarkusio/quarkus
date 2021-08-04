package org.jboss.resteasy.reactive.server.jaxrs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.common.util.PathSegmentImpl;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;
import org.jboss.resteasy.reactive.common.util.URIDecoder;
import org.jboss.resteasy.reactive.common.util.UnmodifiableMultivaluedMap;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.UriMatch;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.spi.ServerHttpRequest;

/**
 * UriInfo implementation
 */
public class UriInfoImpl implements UriInfo {

    private final ResteasyReactiveRequestContext currentRequest;
    private MultivaluedMap<String, String> queryParams;
    private MultivaluedMap<String, String> pathParams;
    private URI requestUri;

    public UriInfoImpl(ResteasyReactiveRequestContext currentRequest) {
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
        String path = URIDecoder.decodeURIComponent(currentRequest.getPath(), false);
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
            ServerHttpRequest request = currentRequest.serverRequest();
            try {
                // TCK says normalized
                requestUri = new URI(currentRequest.getAbsoluteURI())
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
        try {
            // TCK says normalized
            return new URI(currentRequest.getAbsoluteURI()).normalize();
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
        try {
            Deployment deployment = currentRequest.getDeployment();
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
            return new URI(currentRequest.getScheme(), currentRequest.getAuthority(),
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
            RuntimeResource target = currentRequest.getTarget();
            if (target != null) { // a target can be null if this happens in a filter that runs before the target is set
                for (Entry<String, Integer> pathParam : target.getPathParameterIndexes().entrySet()) {
                    pathParams.add(pathParam.getKey(), currentRequest.getPathParam(pathParam.getValue()));
                }
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
            Collection<String> entries = currentRequest.serverRequest().queryParamNames();
            for (String i : entries) {
                queryParams.addAll(i, currentRequest.serverRequest().getAllQueryParams(i));
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
