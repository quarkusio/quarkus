package org.jboss.resteasy.reactive.common.jaxrs;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class LinkBuilderImpl implements Link.Builder {
    /**
     * A map for all the link parameters such as "rel", "type", etc.
     */
    protected final Map<String, String> map = new HashMap<String, String>();
    protected UriBuilder uriBuilder;
    protected URI baseUri;

    @Override
    public Link.Builder link(Link link) {
        uriBuilder = UriBuilder.fromUri(link.getUri());
        this.map.clear();
        this.map.putAll(link.getParams());
        return this;
    }

    @Override
    public Link.Builder link(String link) {
        Link l = LinkImpl.valueOf(link);
        return link(l);
    }

    @Override
    public Link.Builder uriBuilder(UriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder.clone();
        return this;
    }

    @Override
    public Link.Builder uri(URI uri) {
        if (uri == null)
            throw new IllegalArgumentException("URI was null");
        uriBuilder = UriBuilder.fromUri(uri);
        return this;
    }

    @Override
    public Link.Builder uri(String uri) throws IllegalArgumentException {
        if (uri == null)
            throw new IllegalArgumentException("URI was null");
        uriBuilder = UriBuilder.fromUri(uri);
        return this;
    }

    @Override
    public Link.Builder rel(String rel) {
        if (rel == null)
            throw new IllegalArgumentException("param was null");
        final String rels = this.map.get(Link.REL);
        param(Link.REL, rels == null ? rel : rels + " " + rel);
        return this;
    }

    @Override
    public Link.Builder title(String title) {
        if (title == null)
            throw new IllegalArgumentException("param was null");
        param(Link.TITLE, title);
        return this;

    }

    @Override
    public Link.Builder type(String type) {
        if (type == null)
            throw new IllegalArgumentException("param was null");
        param(Link.TYPE, type);
        return this;
    }

    @Override
    public Link.Builder param(String name, String value) throws IllegalArgumentException {
        if (name == null)
            throw new IllegalArgumentException("param was null");
        if (value == null)
            throw new IllegalArgumentException("param was null");
        this.map.put(name, value);
        return this;
    }

    @Override
    public Link build(Object... values) throws UriBuilderException {
        if (values == null)
            throw new IllegalArgumentException("param was null");
        URI built = null;
        if (uriBuilder == null) {
            built = baseUri;
        } else {
            built = this.uriBuilder.build(values);
        }
        if (!built.isAbsolute() && baseUri != null) {
            built = baseUri.resolve(built);
        }
        return new LinkImpl(built, this.map);
    }

    @Override
    public Link buildRelativized(URI uri, Object... values) {
        if (uri == null)
            throw new IllegalArgumentException("URI was null");
        if (values == null)
            throw new IllegalArgumentException("values was null");
        URI built = uriBuilder.build(values);
        URI with = built;
        if (baseUri != null)
            with = baseUri.resolve(built);
        return new LinkImpl(uri.relativize(with), this.map);
    }

    @Override
    public Link.Builder baseUri(URI uri) {
        this.baseUri = uri;
        return this;
    }

    @Override
    public Link.Builder baseUri(String uri) {
        this.baseUri = URI.create(uri);
        return this;
    }
}
