package io.quarkus.resteasy.reactive.links.runtime;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.common.util.MultivaluedTreeMap;

import io.quarkus.runtime.annotations.RecordableConstructor;

/**
 * A container holding links mapped by an entity which they represent.
 */
public final class LinksContainer {

    /**
     * Links mapped by their entity type.
     * In order to be recorded this field name has to match the constructor parameter name and have a getter.
     */
    private final MultivaluedMap<String, LinkInfo> linksMap;

    public LinksContainer() {
        linksMap = new MultivaluedTreeMap<>();
    }

    @RecordableConstructor
    public LinksContainer(MultivaluedMap<String, LinkInfo> linksMap) {
        this.linksMap = linksMap;
    }

    public List<LinkInfo> getForClass(Class<?> c) {
        return linksMap.getOrDefault(c.getName(), Collections.emptyList());
    }

    public void put(LinkInfo linkInfo) {
        linksMap.add(linkInfo.getEntityType(), linkInfo);
    }

    public MultivaluedMap<String, LinkInfo> getLinksMap() {
        return MultivaluedTreeMap.clone(linksMap);
    }
}
