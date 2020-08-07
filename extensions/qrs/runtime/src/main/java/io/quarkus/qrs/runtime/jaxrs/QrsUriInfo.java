package io.quarkus.qrs.runtime.jaxrs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.UriMatch;
import io.quarkus.qrs.runtime.spi.BeanFactory;
import io.vertx.core.http.HttpServerRequest;

/**
 * UriInfo implementation
 */
public class QrsUriInfo implements UriInfo {

    private final QrsRequestContext currentRequest;
    private URI requestUri;

    public QrsUriInfo(QrsRequestContext currentRequest) {
        this.currentRequest = currentRequest;
    }

    @Override
    public String getPath() {
        return currentRequest.getContext().request().path();
    }

    @Override
    public String getPath(boolean decode) {
        //TODO: care about the decode parameter
        return currentRequest.getContext().request().path();
    }

    @Override
    public List<PathSegment> getPathSegments() {
        return getPathSegments(true);
    }

    @Override
    public List<PathSegment> getPathSegments(boolean decode) {
        return null;
    }

    @Override
    public URI getRequestUri() {
        if (requestUri == null) {
            HttpServerRequest request = currentRequest.getContext().request();
            try {
                requestUri = new URI(request.absoluteURI() + (request.query() == null ? "" : "?" + request.query()));
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
        return null;
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        return null;
    }

    @Override
    public URI getBaseUri() {
        return null;
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        return null;
    }

    @Override
    public List<String> getMatchedURIs() {
        return getMatchedURIs(true);
    }

    @Override
    public List<String> getMatchedURIs(boolean decode) {
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
            BeanFactory.BeanInstance<?> target = oldMatches.get(i).target;
            if (target != null) {
                matched.add(target.getInstance());
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
