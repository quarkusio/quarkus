package org.jboss.resteasy.reactive.common.headers;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkHeaders {
    private final Map<String, Link> linksByRelationship = new HashMap<>();
    private final List<Link> links = new ArrayList<>();

    public LinkHeaders(MultivaluedMap<String, Object> headers) {
        List<Object> values = headers.get("Link");
        if (values == null) {
            return;
        }

        for (Object val : values) {
            if (val instanceof Link) {
                addLink((Link) val);
            } else if (val instanceof String) {
                for (String link : ((String) val).split(",")) {
                    addLink(Link.valueOf(link));
                }
            } else {
                String str = HeaderUtil.headerToString(val);
                addLink(Link.valueOf(str));
            }
        }
    }

    private void addLink(final Link link) {
        links.add(link);
        for (String rel : link.getRels()) {
            linksByRelationship.put(rel, link);
        }
    }

    public Link getLinkByRelationship(String rel) {
        return linksByRelationship.get(rel);
    }

    public List<Link> getLinks() {
        return links;
    }

}
