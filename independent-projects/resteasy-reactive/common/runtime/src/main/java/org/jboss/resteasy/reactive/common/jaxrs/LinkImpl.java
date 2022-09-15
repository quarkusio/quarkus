package org.jboss.resteasy.reactive.common.jaxrs;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.RuntimeDelegate;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class LinkImpl extends Link {
    protected final URI uri;

    /**
     * A map for all the link parameters such as "rel", "type", etc.
     */
    protected final Map<String, String> map;

    protected static final RuntimeDelegate.HeaderDelegate<Link> delegate = RuntimeDelegate.getInstance()
            .createHeaderDelegate(Link.class);

    public static Link valueOf(String value) {
        return delegate.fromString(value);
    }

    LinkImpl(final URI uri, final Map<String, String> map) {
        this.uri = uri;
        this.map = map.isEmpty() ? Collections.<String, String> emptyMap()
                : Collections
                        .unmodifiableMap(new HashMap<String, String>(map));
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public UriBuilder getUriBuilder() {
        return UriBuilder.fromUri(uri);
    }

    @Override
    public String getRel() {
        return map.get(REL);
    }

    @Override
    public List<String> getRels() {
        final String rels = map.get(REL);
        return rels == null ? Collections.<String> emptyList() : Arrays.asList(rels.split(" +"));
    }

    @Override
    public String getTitle() {
        return map.get(TITLE);
    }

    @Override
    public String getType() {
        return map.get(TYPE);
    }

    @Override
    public Map<String, String> getParams() {
        return map;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof LinkImpl) {
            final LinkImpl otherLink = (LinkImpl) other;
            return uri.equals(otherLink.uri) && map.equals(otherLink.map);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + (this.uri != null ? this.uri.hashCode() : 0);
        hash = 89 * hash + (this.map != null ? this.map.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return delegate.toString(this);
    }

}
