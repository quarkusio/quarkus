package io.quarkus.resteasy.reactive.links.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.common.util.MultivaluedTreeMap;

/**
 * A container holding links mapped by an entity which they represent.
 */
public final class LinksContainer {

    /**
     * Links mapped by their entity type.
     */
    private final MultivaluedMap<String, LinkInfo> linksMap;

    public LinksContainer() {
        linksMap = new MultivaluedTreeMap<>();
    }

    public LinksContainer(Map<String, List<LinkInfo>> linksMap) {
        this.linksMap = new MultivaluedTreeMap<>();
        for (Map.Entry<String, List<LinkInfo>> entry : linksMap.entrySet()) {
            this.linksMap.addAll(entry.getKey(), entry.getValue());
        }
    }

    public List<LinkInfo> getForClass(Class<?> c) {
        return linksMap.getOrDefault(c.getName(), Collections.emptyList());
    }

    public void put(LinkInfo linkInfo) {
        linksMap.add(linkInfo.entityType(), linkInfo);
    }

    public MultivaluedMap<String, LinkInfo> getLinksMap() {
        return MultivaluedTreeMap.clone(linksMap);
    }
}
